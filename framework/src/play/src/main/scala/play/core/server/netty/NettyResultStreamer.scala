package play.core.server.netty

import play.api.mvc._
import play.api.libs.iteratee._
import play.api._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._

import com.typesafe.netty.http.pipelining.{ OrderedDownstreamMessageEvent, OrderedUpstreamMessageEvent }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

/**
 * Streams Play results to Netty
 */
object NettyResultStreamer {

  implicit val internalExecutionContext = play.core.Execution.internalContext

  /**
   * Send the result to netty
   *
   * @return A Future that will be redeemed when the result is completely sent
   */
  def sendResult(result: SimpleResult, closeConnection: Boolean, httpVersion: HttpVersion, startSequence: Int)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Future[_] = {
    val nettyResponse = createNettyResponse(result.header, closeConnection, httpVersion)

    // Result of this iteratee is whether the connection must be closed
    val bodyIteratee: Iteratee[Array[Byte], Boolean] = result match {

      // Sanitisation: ensure that we don't send chunked responses to HTTP 1.0 requests
      case UsesTransferEncoding() if httpVersion == HttpVersion.HTTP_1_0 => {
        // Make sure enumerator knows it's done, so that any resources it uses can be cleaned up
        result.body |>> Done(())

        val error = Results.HttpVersionNotSupported("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
        nettyStreamIteratee(createNettyResponse(error.header, closeConnection, httpVersion), startSequence)
          .map(_ => closeConnection)(internalExecutionContext)
      }

      case CloseConnection() => {
        nettyStreamIteratee(nettyResponse, startSequence).map(_ => true)(internalExecutionContext)
      }

      case EndOfBodyInProtocol() => {
        nettyStreamIteratee(nettyResponse, startSequence).map(_ => closeConnection)(internalExecutionContext)
      }

      case _ => {
        bufferingIteratee(nettyResponse, startSequence, closeConnection, httpVersion)
      }

    }

    // Clean up
    val sentResponse = result.body |>>> bodyIteratee
    sentResponse.onComplete {
      case Success(mustCloseConnection) =>
        if (mustCloseConnection) Channels.close(e.getChannel)
      case Failure(ex) =>
        Play.logger.debug(ex.toString)
        Channels.close(e.getChannel)
    }
    sentResponse
  }

  /**
   * An iteratee that buffers the first element from the enumerator, and if it then receives EOF, sends the a result
   * immediately with the body and a content length.
   *
   * If there is more than one element from the enumerator, it sends the response either as chunked or as a stream that
   * gets closed, depending on whether the protocol is HTTP 1.0 or HTTP 1.1.
   */
  def bufferingIteratee(nettyResponse: HttpResponse, startSequence: Int, closeConnection: Boolean, httpVersion: HttpVersion)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], Boolean] = {

    // Left is the first chunk if there was more than one chunk, right is the zero or one and only chunk
    def takeUpToOneChunk(chunk: Option[Array[Byte]]): Iteratee[Array[Byte], Either[Array[Byte], Option[Array[Byte]]]] = Cont {
      // We have a second chunk, fail with left
      case in @ Input.El(data) if chunk.isDefined => Done(Left(chunk.get), in)
      // This is the first chunk
      case Input.El(data) => takeUpToOneChunk(Some(data))
      case Input.Empty => takeUpToOneChunk(chunk)
      // We reached EOF, which means we either have one or zero chunks
      case Input.EOF => Done(Right(chunk))
    }

    takeUpToOneChunk(None).flatMap {
      case Right(chunk) => {
        val buffer = chunk.map(ChannelBuffers.wrappedBuffer).getOrElse(ChannelBuffers.EMPTY_BUFFER)
        // We successfully buffered it, so set the content length and send the whole thing as one buffer
        nettyResponse.setHeader(CONTENT_LENGTH, buffer.readableBytes)
        nettyResponse.setContent(buffer)
        val promise = NettyPromise(sendDownstream(startSequence, true, nettyResponse))
        Iteratee.flatten(promise.map {
          _ => Done[Array[Byte], Boolean](closeConnection)
        }.recover {
          case _ => Done[Array[Byte], Boolean](closeConnection)
        })
      }
      case Left(chunk) => {
        val bufferedAsEnumerator = Enumerator(chunk)

        // Get the iteratee, maybe chunked or maybe not according HTTP version
        val bodyIteratee = if (httpVersion == HttpVersion.HTTP_1_0) {
          nettyStreamIteratee(nettyResponse, startSequence).map(_ => true)(internalExecutionContext)
        } else {
          // Chunk it
          nettyResponse.setHeader(TRANSFER_ENCODING, CHUNKED)
          Results.chunk &>> nettyStreamIteratee(nettyResponse, startSequence).map(_ => closeConnection)(internalExecutionContext)
        }

        // Feed the buffered content into the iteratee, and return the iteratee so that future content can continue
        // to be fed directly in as normal
        Iteratee.flatten(bufferedAsEnumerator |>> bodyIteratee)
      }
    }

  }

  def nettyStreamIteratee(nettyResponse: HttpResponse, startSequence: Int)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], Unit] = {

    def step(subsequence: Int)(in: Input[Array[Byte]]): Iteratee[Array[Byte], Unit] = in match {
      case Input.El(x) =>
        val b = ChannelBuffers.wrappedBuffer(x)
        nextWhenComplete(sendDownstream(subsequence, false, b), step(subsequence + 1), ())
      case Input.Empty =>
        Cont(step(subsequence))
      case Input.EOF =>
        sendDownstream(subsequence, true, ChannelBuffers.EMPTY_BUFFER)
        Done(())
    }
    nextWhenComplete(sendDownstream(startSequence, false, nettyResponse), step(startSequence + 1), ())
  }

  def createNettyResponse(header: ResponseHeader, closeConnection: Boolean, httpVersion: HttpVersion) = {
    val nettyResponse = new DefaultHttpResponse(httpVersion, HttpResponseStatus.valueOf(header.status))

    import scala.collection.JavaConverters._

    // Set response headers
    header.headers.foreach {

      // Fix a bug for Set-Cookie header.
      // Multiple cookies could be merged in a single header
      // but it's not properly supported by some browsers
      case (name @ play.api.http.HeaderNames.SET_COOKIE, value) => {
        nettyResponse.setHeader(name, Cookies.decode(value).map { c => Cookies.encode(Seq(c)) }.asJava)
      }

      case (name, value) => nettyResponse.setHeader(name, value)
    }

    // Response header Connection: Keep-Alive is needed for HTTP 1.0
    if (!closeConnection && httpVersion == HttpVersion.HTTP_1_0) {
      nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
    }

    nettyResponse
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamMessageEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }

  def nextWhenComplete[E, A](future: ChannelFuture, step: (Input[E]) => Iteratee[E, A], done: A)(implicit ctx: ChannelHandlerContext): Iteratee[E, A] = {
    // If the channel isn't currently connected, then this future will never be redeemed.  This is racey, and impossible
    // to 100% detect, but it's better to fail fast if possible than to sit there waiting forever
    if (!ctx.getChannel.isConnected) {
      Done(done)
    } else {
      Iteratee.flatten(
        NettyPromise(future).map[Iteratee[E, A]] {
          _ => if (ctx.getChannel.isConnected()) Cont(step) else Done(done)
        }.recover[Iteratee[E, A]] {
          case _ => Done(done)
        }
      )
    }
  }

  /**
   * Extractor object that determines whether the end of the body is specified by the protocol
   */
  object EndOfBodyInProtocol {
    def unapply(result: SimpleResult): Boolean = {
      import result.header.headers
      headers.contains(TRANSFER_ENCODING) || headers.contains(CONTENT_LENGTH)
    }
  }

  /**
   * Extractor object that determines whether the result specifies that the connection should be closed
   */
  object CloseConnection {
    def unapply(result: SimpleResult): Boolean = result.connection == HttpConnection.Close
  }

  /**
   * Extractor object that determines whether the result uses a transfer encoding
   */
  object UsesTransferEncoding {
    def unapply(result: SimpleResult): Boolean = result.header.headers.contains(TRANSFER_ENCODING)
  }

}
