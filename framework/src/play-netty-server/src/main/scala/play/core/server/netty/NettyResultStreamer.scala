/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import play.api.mvc._
import play.api.libs.iteratee._
import play.api._
import play.core.server.common.ServerResultUtils
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * Streams Play results to Netty
 */
object NettyResultStreamer {

  import NettyFuture._

  private val logger = Logger(NettyResultStreamer.getClass)

  // A channel status holds whether the connection must be closed and the last subsequence sent
  class ChannelStatus(val closeConnection: Boolean, val lastSubsequence: Int)

  /**
   * Send the result to netty
   *
   * @return A Future that will be redeemed when the result is completely sent
   */
  def sendResult(requestHeader: RequestHeader, result: Result, httpVersion: HttpVersion, startSequence: Int)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent): Future[_] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    // Break out sending logic because when the first result is invalid we may
    // need to call send again with an error result.
    def send(result: Result): Future[ChannelStatus] = {
      // Result of this iteratee is a completion status
      val resultStreaming = ServerResultUtils.determineResultStreaming(requestHeader, result)
      resultStreaming.flatMap {
        case Left(ServerResultUtils.InvalidResult(reason, alternativeResult)) =>
          logger.warn(s"Cannot send result, sending error result instead: $reason")
          send(alternativeResult)
        case Right((streaming, connectionHeader)) =>
          // Create our base response. It may be modified, depending on the
          // streaming strategy.
          val nettyResponse = createNettyResponse(result.header, connectionHeader, httpVersion)

          // Streams whatever content that has been added to the nettyResponse, or
          // no content if none was added
          def sendContent() = {
            val future = sendDownstream(startSequence, !connectionHeader.willClose, nettyResponse).toScala
            val channelStatus = new ChannelStatus(connectionHeader.willClose, startSequence)
            future.map(_ => channelStatus).recover { case _ => channelStatus }
          }

          // Streams the value of an enumerator into the nettyResponse
          def streamEnum(enum: Enumerator[Array[Byte]]) = {
            enum |>>> nettyStreamIteratee(nettyResponse, startSequence, connectionHeader.willClose)
          }

          // Interpret the streaming strategy for Netty
          streaming match {
            case ServerResultUtils.StreamWithClose(enum) =>
              assert(connectionHeader.willClose)
              streamEnum(enum)
            case ServerResultUtils.StreamWithKnownLength(enum) =>
              streamEnum(enum)
            case ServerResultUtils.StreamWithNoBody =>
              // `StreamWithNoBody` won't add the Content-Length entity-header to the response (if not already present) 
              sendContent()
            case ServerResultUtils.StreamWithStrictBody(body) =>
              // We successfully buffered it, so set the content length and send the whole thing as one buffer
              val buffer = if (body.isEmpty) ChannelBuffers.EMPTY_BUFFER else ChannelBuffers.wrappedBuffer(body)
              nettyResponse.headers().set(CONTENT_LENGTH, buffer.readableBytes)
              nettyResponse.setContent(buffer)
              sendContent()
            case ServerResultUtils.UseExistingTransferEncoding(enum) =>
              streamEnum(enum)
            case ServerResultUtils.PerformChunkedTransferEncoding(transferEncodedEnum) =>
              nettyResponse.headers().set(TRANSFER_ENCODING, CHUNKED)
              streamEnum(transferEncodedEnum &> Results.chunk)
          }
      }
    }
    val sentResponse: Future[ChannelStatus] = send(result)

    // Clean up
    sentResponse.onComplete {
      case Success(cs: ChannelStatus) =>
        if (cs.closeConnection) {
          // Close in an orderely fashion.
          val channel = oue.getChannel;
          val closeEvent = new DownstreamChannelStateEvent(
            channel, channel.getCloseFuture, ChannelState.OPEN, java.lang.Boolean.FALSE);
          val ode = new OrderedDownstreamChannelEvent(oue, cs.lastSubsequence + 1, true, closeEvent)
          ctx.sendDownstream(ode)
        }
      case Failure(ex) =>
        logger.error("Error while sending response.", ex)
        Channels.close(oue.getChannel)
    }
    sentResponse
  }

  // Construct an iteratee for the purposes of streaming responses to a downstream handler.
  private def nettyStreamIteratee(nettyResponse: HttpResponse, startSequence: Int, closeConnection: Boolean)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[Array[Byte], ChannelStatus] = {

    def step(subsequence: Int)(in: Input[Array[Byte]]): Iteratee[Array[Byte], ChannelStatus] = in match {
      case Input.El(x) =>
        val b = ChannelBuffers.wrappedBuffer(x)
        nextWhenComplete(sendDownstream(subsequence, false, b), step(subsequence + 1), new ChannelStatus(closeConnection, subsequence))
      case Input.Empty =>
        Cont(step(subsequence))
      case Input.EOF =>
        sendDownstream(subsequence, !closeConnection, ChannelBuffers.EMPTY_BUFFER)
        Done(new ChannelStatus(closeConnection, subsequence))
    }
    nextWhenComplete(sendDownstream(startSequence, false, nettyResponse), step(startSequence + 1), new ChannelStatus(closeConnection, startSequence))
  }

  def createNettyResponse(header: ResponseHeader, connectionHeader: ServerResultUtils.ConnectionHeader, httpVersion: HttpVersion) = {
    val nettyResponse = new DefaultHttpResponse(httpVersion, HttpResponseStatus.valueOf(header.status))

    import scala.collection.JavaConverters._

    // Set response headers
    val headers = ServerResultUtils.splitHeadersIntoSeq(header.headers)
    try {
      headers.foreach {
        case (name, value) => nettyResponse.headers().add(name, value)
      }
    } catch {
      case NonFatal(e) =>
        if (logger.isErrorEnabled) {
          val prettyHeaders = headers.map { case (name, value) => s"$name -> $value" }.mkString("[", ",", "]")
          val msg = s"Exception occurred while setting response's headers to $prettyHeaders. Action taken is to set the response's status to ${HttpResponseStatus.INTERNAL_SERVER_ERROR} and discard all headers."
          logger.error(msg, e)
        }
        nettyResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        nettyResponse.headers().clear()
    }

    connectionHeader.header.foreach(headerValue => nettyResponse.headers().set(CONNECTION, headerValue))

    nettyResponse
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamChannelEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }

  def nextWhenComplete[E, A](future: ChannelFuture, step: (Input[E]) => Iteratee[E, A], done: A)(implicit ctx: ChannelHandlerContext): Iteratee[E, A] = {
    // If the channel isn't currently connected, then this future will never be redeemed.  This is racey, and impossible
    // to 100% detect, but it's better to fail fast if possible than to sit there waiting forever
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    if (!ctx.getChannel.isConnected) {
      Done(done)
    } else {
      Iteratee.flatten(
        future.toScala.map[Iteratee[E, A]] {
          _ => if (ctx.getChannel.isConnected()) Cont(step) else Done(done)
        }.recover[Iteratee[E, A]] {
          case _ => Done(done)
        }
      )
    }
  }

}
