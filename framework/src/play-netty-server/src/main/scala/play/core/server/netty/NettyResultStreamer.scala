/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import play.api.mvc._
import play.api.libs.iteratee._
import play.api._
import play.api.http.HttpErrorHandler
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

  // cache the date header of the last response so we only need to compute it every second
  private[this] var cachedDateHeader: (Long, String) = (Long.MinValue, null)
  private[this] def dateHeader: String = {
    val currentTimeMillis = System.currentTimeMillis()
    val currentTimeSeconds = currentTimeMillis / 1000
    cachedDateHeader match {
      case (cachedSeconds, dateHeaderString) if cachedSeconds == currentTimeSeconds =>
        dateHeaderString
      case _ =>
        val dateHeaderString = ResponseHeader.httpDateFormat.print(currentTimeMillis)
        cachedDateHeader = currentTimeSeconds -> dateHeaderString
        dateHeaderString
    }
  }

  /**
   * Send the result to netty
   *
   * @return A Future that will be redeemed when the result is completely sent
   */
  def sendResult(
    requestHeader: RequestHeader,
    result: Result,
    httpVersion: HttpVersion,
    startSequence: Int,
    errorHandler: HttpErrorHandler)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent): Future[_] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    // Break out sending logic because when the first result is invalid we may
    // need to call send again with an error result.
    def send(result: Result): Future[ChannelStatus] = {
      // Result of this iteratee is a completion status
      createNettyResponse(requestHeader, result, httpVersion, errorHandler).flatMap {
        case (nettyResponse, streaming, connectionHeader) =>

          // Streams whatever content that has been added to the nettyResponse, or
          // no content if none was added
          def sendContent(): Future[ChannelStatus] = {
            val future = sendDownstream(startSequence, !connectionHeader.willClose, nettyResponse).toScala
            val channelStatus = new ChannelStatus(connectionHeader.willClose, startSequence)
            future.map(_ => channelStatus).recover { case _ => channelStatus }
          }

          // Streams the value of an enumerator into the nettyResponse
          def streamEnum(enum: Enumerator[Array[Byte]]): Future[ChannelStatus] = {
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
              // Ensure that the body is consumed in case any onDoneEnumerating handlers are registered
              result.body |>>> Iteratee.ignore
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
          val channel = oue.getChannel
          val closeEvent = new DownstreamChannelStateEvent(
            channel, channel.getCloseFuture, ChannelState.OPEN, java.lang.Boolean.FALSE)
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

  /**
   * Create a Netty HttpResponse from a Play Result object. This method may call itself
   * recursively if the first attempt to create a response fails. The HttpResponse may
   * be modified later, depending on the streaming strategy.
   */
  def createNettyResponse(
    requestHeader: RequestHeader,
    result: Result,
    httpVersion: HttpVersion,
    errorHandler: HttpErrorHandler): Future[(HttpResponse, ServerResultUtils.ResultStreaming, ServerResultUtils.ConnectionHeader)] = {

    // Wrap the conversion logic with some error handling
    ServerResultUtils.resultConversionWithErrorHandling(requestHeader, result, errorHandler) { result =>

      import play.api.libs.iteratee.Execution.Implicits.trampoline

      val resultStreaming = ServerResultUtils.determineResultStreaming(requestHeader, result, errorHandler)
      resultStreaming.flatMap {
        case Left(ServerResultUtils.InvalidResult(reason, alternativeResult)) =>
          logger.warn(s"Cannot send result, sending error result instead: $reason")
          // Try conversion again with new result
          createNettyResponse(requestHeader, alternativeResult, httpVersion, errorHandler)
        case Right((streaming, connectionHeader)) =>

          val responseStatus: HttpResponseStatus = result.header.reasonPhrase match {
            case Some(phrase) => new HttpResponseStatus(result.header.status, phrase)
            case None => HttpResponseStatus.valueOf(result.header.status)
          }

          val nettyResponse = new DefaultHttpResponse(httpVersion, responseStatus)
          // Set response headers
          val nettyHeaders = nettyResponse.headers()
          ServerResultUtils.splitSetCookieHeaders(result.header.headers).foreach {
            case (name, value) => nettyHeaders.add(name, value)
          }
          connectionHeader.header foreach { headerValue =>
            nettyHeaders.set(CONNECTION, headerValue)
          }
          // Netty doesn't add the required Date header for us, so make sure there is one here
          if (!nettyHeaders.contains(DATE)) {
            nettyHeaders.add(DATE, dateHeader)
          }
          Future.successful((nettyResponse, streaming, connectionHeader))
      }
    } {
      // Fallback response if an unrecoverable error occurs
      val nettyResponse = new DefaultHttpResponse(httpVersion, HttpResponseStatus.INTERNAL_SERVER_ERROR)
      HttpHeaders.setContentLength(nettyResponse, 0)
      nettyResponse.headers().add(DATE, dateHeader)
      nettyResponse.headers().add(CONNECTION, "close")
      val emptyBytes = new Array[Byte](0)
      (nettyResponse, ServerResultUtils.StreamWithStrictBody(emptyBytes), ServerResultUtils.SendClose)
    }
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
