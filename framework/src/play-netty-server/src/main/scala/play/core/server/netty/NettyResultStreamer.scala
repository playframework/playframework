/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.http.{ Status, HttpChunk, HttpEntity }
import play.api.libs.streams.Streams
import play.api.mvc._
import play.api.libs.iteratee._
import play.api._
import play.core.server.common.ServerResultUtils
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{ HttpChunk => NettyChunk, _ }
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

import play.api.libs.iteratee.Execution.Implicits.trampoline

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
  def sendResult(requestHeader: RequestHeader, unvalidated: Result, httpVersion: HttpVersion, startSequence: Int)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent, mat: Materializer): Future[_] = {

    val result = ServerResultUtils.validateResult(requestHeader, unvalidated)

    val connectionHeader = ServerResultUtils.determineConnectionHeader(requestHeader, result)
    val skipEntity = requestHeader.method == HttpMethod.HEAD.getName

    val nettyResponse = createNettyResponse(result, connectionHeader.header, httpVersion)

    // Streams whatever content that has been added to the nettyResponse, or
    // no content if none was added
    def sendResponse(response: HttpResponse, closeConnection: Boolean) = {
      val future = sendDownstream(startSequence, !closeConnection, response).toScala
      val channelStatus = new ChannelStatus(connectionHeader.willClose, startSequence)
      future.map(_ => channelStatus).recover { case _ => channelStatus }
    }

    val sentResponse: Future[ChannelStatus] = nettyResponse match {
      case Left(errorResponse) =>
        sendResponse(errorResponse, true)

      case Right(response) =>

        result.body match {

          case any if skipEntity =>
            ServerResultUtils.cancelEntity(any)
            sendResponse(response, connectionHeader.willClose)

          case HttpEntity.Strict(data, _) =>
            val buffer = if (data.isEmpty) ChannelBuffers.EMPTY_BUFFER else ChannelBuffers.wrappedBuffer(data.asByteBuffer)
            response.setContent(buffer)
            sendResponse(response, connectionHeader.willClose)

          case HttpEntity.Streamed(data, _, _) =>
            Streams.publisherToEnumerator(data.runWith(Sink.publisher)) |>>>
              nettyStreamIteratee(response, startSequence, connectionHeader.willClose)

          case HttpEntity.Chunked(chunks, _) =>
            HttpHeaders.setTransferEncodingChunked(response)
            Streams.publisherToEnumerator(chunks.runWith(Sink.publisher)) |>>>
              nettyChunkedIteratee(response, startSequence, connectionHeader.willClose)

        }
    }

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
  private def nettyStreamIteratee(nettyResponse: HttpResponse, startSequence: Int, closeConnection: Boolean)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[ByteString, ChannelStatus] = {

    def step(subsequence: Int)(in: Input[ByteString]): Iteratee[ByteString, ChannelStatus] = in match {
      case Input.El(bytes) =>
        val buffer = ChannelBuffers.wrappedBuffer(bytes.asByteBuffer)
        nextWhenComplete(sendDownstream(subsequence, false, buffer), step(subsequence + 1), new ChannelStatus(closeConnection, subsequence))
      case Input.Empty =>
        Cont(step(subsequence))
      case Input.EOF =>
        doneOnComplete(
          sendDownstream(subsequence, !closeConnection, ChannelBuffers.EMPTY_BUFFER),
          new ChannelStatus(closeConnection, subsequence)
        )
    }
    nextWhenComplete(sendDownstream(startSequence, false, nettyResponse), step(startSequence + 1), new ChannelStatus(closeConnection, startSequence))
  }

  private def nettyChunkedIteratee(nettyResponse: HttpResponse, startSequence: Int, closeConnection: Boolean)(implicit ctx: ChannelHandlerContext, e: OrderedUpstreamMessageEvent): Iteratee[HttpChunk, ChannelStatus] = {

    def step(subsequence: Int)(in: Input[HttpChunk]): Iteratee[HttpChunk, ChannelStatus] = in match {

      case Input.El(HttpChunk.Chunk(bytes)) =>
        val chunk = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(bytes.asByteBuffer))
        nextWhenComplete(sendDownstream(subsequence, false, chunk), step(subsequence + 1), new ChannelStatus(closeConnection, subsequence))

      case Input.El(HttpChunk.LastChunk(trailers)) =>
        val lastChunk = new DefaultHttpChunkTrailer
        trailers.headers.foreach {
          case (name, value) =>
            lastChunk.trailingHeaders().add(name, value)
        }
        doneOnComplete(
          sendDownstream(subsequence, !closeConnection, lastChunk),
          new ChannelStatus(closeConnection, subsequence)
        )

      case Input.EOF =>
        doneOnComplete(
          sendDownstream(subsequence, !closeConnection, NettyChunk.LAST_CHUNK),
          new ChannelStatus(closeConnection, subsequence)
        )

      case Input.Empty =>
        Cont(step(subsequence))
    }
    nextWhenComplete(sendDownstream(startSequence, false, nettyResponse), step(startSequence + 1), new ChannelStatus(closeConnection, startSequence))
  }

  def createNettyResponse(result: Result, connectionHeader: Option[String], httpVersion: HttpVersion): Either[HttpResponse, HttpResponse] = {
    val responseStatus = result.header.reasonPhrase match {
      case Some(phrase) => new HttpResponseStatus(result.header.status, phrase)
      case None => HttpResponseStatus.valueOf(result.header.status)
    }
    val nettyResponse = new DefaultHttpResponse(httpVersion, responseStatus)
    val nettyHeaders = nettyResponse.headers()

    // Set response headers
    val headers = ServerResultUtils.splitSetCookieHeaders(result.header.headers)
    try {
      headers foreach {
        case (name, value) => nettyHeaders.add(name, value)
      }

      // Content type and length
      if (mayHaveContentLength(result.header.status)) {
        result.body.contentLength.foreach { contentLength =>
          if (nettyHeaders.contains(CONTENT_LENGTH)) {
            logger.warn("Content-Length header was set manually in the header, ignoring manual header")
            nettyHeaders.set(CONTENT_LENGTH, contentLength)
          } else {
            nettyHeaders.add(CONTENT_LENGTH, contentLength)
          }
        }
      }
      result.body.contentType.foreach { contentType =>
        if (nettyHeaders.contains(CONTENT_TYPE)) {
          logger.warn(s"Content-Type set both in header (${nettyHeaders.get(CONTENT_TYPE)}) and attached to entity ($contentType), ignoring content type from entity. To remove this warning, use Result.as(...) to set the content type, rather than setting the header manually.")
        } else {
          nettyHeaders.add(CONTENT_TYPE, contentType)
        }
      }

      connectionHeader.foreach { headerValue =>
        nettyHeaders.set(CONNECTION, headerValue)
      }

      // Netty doesn't add the required Date header for us, so make sure there is one here
      if (!nettyHeaders.contains(DATE)) {
        nettyHeaders.add(DATE, dateHeader)
      }

      Right(nettyResponse)
    } catch {
      case NonFatal(e) =>
        if (logger.isErrorEnabled) {
          val prettyHeaders = headers.map { case (name, value) => s"$name -> $value" }.mkString("[", ",", "]")
          val msg = s"Exception occurred while setting response's headers to $prettyHeaders. Action taken is to set the response's status to ${HttpResponseStatus.INTERNAL_SERVER_ERROR} and discard all headers."
          logger.error(msg, e)
        }
        nettyResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        nettyHeaders.clear()
        HttpHeaders.setContentLength(nettyResponse, 0)
        nettyHeaders.add(DATE, dateHeader)
        nettyHeaders.add(CONNECTION, CLOSE)
        Left(nettyResponse)
    }
  }

  private def mayHaveContentLength(status: Int) =
    status != Status.NO_CONTENT && status != Status.NOT_MODIFIED

  def sendDownstream(subSequence: Int, last: Boolean, message: AnyRef)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamChannelEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }

  def doneOnComplete[E, A](future: ChannelFuture, done: A): Iteratee[E, A] = {
    Iteratee.flatten(future.toScala.map { _ =>
      Done[E, A](done)
    }).recover {
      case _ => done
    }
  }

  def nextWhenComplete[E, A](future: ChannelFuture, step: (Input[E]) => Iteratee[E, A], done: A)(implicit ctx: ChannelHandlerContext): Iteratee[E, A] = {
    // If the channel isn't currently connected, then this future will never be redeemed.  This is racey, and impossible
    // to 100% detect, but it's better to fail fast if possible than to sit there waiting forever
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
