package play.core.server.netty

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.Logger

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
    val bodyIteratee: Iteratee[Array[Byte], Boolean] = result.header.headers.get(CONTENT_LENGTH).map { contentLength =>

      // Simple case, we have a content length, so we send with no transfer encoding
      simpleResultIteratee(nettyResponse, startSequence).map(_ => closeConnection)

    } getOrElse {

      // We don't have a content length, decide how to send it based on the streaming strategy
      result.streamingStrategy match {
        case StreamingStrategy.Simple => simpleResultIteratee(nettyResponse, startSequence).map(_ => true)

        case StreamingStrategy.Buffer(maxLength) => bufferingIteratee(nettyResponse, startSequence, maxLength, closeConnection, httpVersion)

        case StreamingStrategy.Chunked(trailers) if (httpVersion != HttpVersion.HTTP_1_0) =>
          chunkedIteratee(nettyResponse, startSequence, trailers).map(_ => true)

        case StreamingStrategy.Chunked(_) => {
          // Make sure enumerator knows it's done, so that any resources it uses can be cleaned up
          result.body |>> Done(())
          // Can't send chunked result if version is HTTP 1.0
          val error = Results.HttpVersionNotSupported("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
          simpleResultIteratee(createNettyResponse(error.header, closeConnection, httpVersion), startSequence)
            .map(_ => closeConnection)
        }
      }
    }

    // Clean up
    val sentResponse = result.body |>>> bodyIteratee
    sentResponse.onComplete {
      case Success(mustCloseConnection) =>
        if (mustCloseConnection) Channels.close(e.getChannel)
      case Failure(ex) =>
        Logger("play").debug(ex.toString)
        Channels.close(e.getChannel)
    }
    sentResponse
  }

  /**
   * An iteratee that buffers the response up to maxLength, and then sends it along with a content length to Netty.
   *
   * If the response is greater than maxLength, sends the response either as chunked or as a stream that gets closed,
   * depending on whether the protocol is HTTP 1.0 or HTTP 1.1.
   */
  def bufferingIteratee(nettyResponse: HttpResponse, startSequence: Int, maxLength: Long, closeConnection: Boolean,
    httpVersion: HttpVersion)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], Boolean] = {

    // Left is the next chunk that didn't fit in our max length, right means we did buffer it successfully
    def takeUpToMaxLength(chunks: List[Array[Byte]], read: Long): Iteratee[Array[Byte], Either[List[Array[Byte]], List[Array[Byte]]]] = Cont {
      case Input.El(data) =>
        if (data.length + read > maxLength) {
          Done(Left(data :: chunks))
        } else {
          takeUpToMaxLength(data :: chunks, read + data.length)
        }
      case Input.Empty => takeUpToMaxLength(chunks, read)
      case Input.EOF => Done(Right((chunks)))
    }

    takeUpToMaxLength(Nil, 0).flatMap {
      case Right(chunks) => {
        val buffer = ChannelBuffers.wrappedBuffer(chunks.reverse: _*)
        // We successfully buffered it, so set the content length and send the whole thing as one buffer
        nettyResponse.setHeader(CONTENT_LENGTH, buffer.readableBytes)
        nettyResponse.setContent(buffer)
        val promise = NettyPromise(sendDownstream(startSequence, true, nettyResponse))
        Iteratee.flatten(promise.map(_ => Done[Array[Byte], Boolean](closeConnection)))
      }
      case Left(chunks) => {
        val bufferedAsEnumerator = Enumerator.enumerate(chunks.reverse)

        // Get the iteratee, maybe chunked or maybe not according HTTP version
        val bodyIteratee = if (httpVersion == HttpVersion.HTTP_1_0) {
          simpleResultIteratee(nettyResponse, startSequence).map(_ => true)
        } else {
          chunkedIteratee(nettyResponse, startSequence, None).map(_ => closeConnection)
        }

        // Feed the buffered content into the iteratee, and return the iteratee so that future content can continue
        // to be fed directly in as normal
        Iteratee.flatten(bufferedAsEnumerator |>> bodyIteratee)
      }
    }

  }

  def simpleResultIteratee(nettyResponse: HttpResponse, startSequence: Int)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], Unit] = {
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

  def chunkedIteratee(nettyResponse: HttpResponse, startSequence: Int, trailers: Option[Iteratee[Array[Byte], Map[String, String]]])(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], _] = {

    nettyResponse.setHeader(TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED)
    nettyResponse.setChunked(true)

    // The iteratee that sends the body to Netty
    val bodyIteratee = {
      def step(subsequence: Int)(in: Input[Array[Byte]]): Iteratee[Array[Byte], Int] = in match {
        case Input.El(x) =>
          val b = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(x))
          nextWhenComplete(sendDownstream(subsequence, false, b), step(subsequence + 1), subsequence)
        case Input.Empty =>
          Cont(step(subsequence))
        case Input.EOF =>
          Done(subsequence)
      }
      nextWhenComplete(sendDownstream(startSequence, false, nettyResponse), step(startSequence + 1), startSequence)
    }

    // Zip the netty iteratee with the trailers iteratee
    val bodyWithLastChunk = trailers.map(trailerIteratee =>
      Enumeratee.zipWith(bodyIteratee, trailerIteratee) { (subsequence, trailers) =>
        val lastChunk = new DefaultHttpChunkTrailer()
        trailers.foreach((lastChunk.addHeader _).tupled)
        (subsequence, lastChunk)
      }
    ).getOrElse(bodyIteratee.map(subsequence => (subsequence, HttpChunk.LAST_CHUNK)))

    // Send the last chunk
    bodyWithLastChunk.mapM { s =>
      if (ctx.getChannel.isConnected()) {
        NettyPromise(sendDownstream(s._1, true, s._2))
      } else {
        Future.successful(())
      }
    }

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
        NettyPromise(future)
          .map[Iteratee[E, A]](_ => if (ctx.getChannel.isConnected()) Cont(step) else Done(done)))
    }
  }

}
