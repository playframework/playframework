/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import java.io.IOException

import akka.util.ByteString
import play.api.http.websocket.Message

import scala.language.reflectiveCalls

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import play.core._
import play.core.server.websocket.WebSocketHandshake
import play.api._
import play.api.http.websocket._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import scala.concurrent.{ Future, Promise }
import scala.concurrent.stm._

import play.core.Execution.Implicits.internalContext
import org.jboss.netty.buffer.{ ChannelBuffers, ChannelBuffer }
import java.util.concurrent.atomic.AtomicInteger

private[server] trait WebSocketHandler {

  import NettyFuture._
  import WebSocketHandler._

  val WebSocketNormalClose = 1000
  val WebSocketUnacceptable = 1003
  val WebSocketMessageTooLong = 1009

  /**
   * The maximum number of messages allowed to be in flight.  Messages can be up to 64K by default, so this number
   * shouldn't be too high.
   */
  private val MaxInFlight = 3

  def newWebSocketInHandler(bufferLimit: Long): (Enumerator[Message], ChannelHandler) = {

    def fromNettyFrame(nettyFrame: WebSocketFrame): Message = nettyFrame match {
      case nettyTextFrame: TextWebSocketFrame =>
        TextMessage(nettyTextFrame.getText)
      case nettyBinaryFrame: BinaryWebSocketFrame =>
        BinaryMessage(ByteString(nettyBinaryFrame.getBinaryData.toByteBuffer))
      case nettyPingFrame: PingWebSocketFrame =>
        PingMessage(ByteString(nettyPingFrame.getBinaryData.toByteBuffer))
      case nettyPongFrame: PongWebSocketFrame =>
        PongMessage(ByteString(nettyPongFrame.getBinaryData.toByteBuffer))
      case nettyCloseFrame: CloseWebSocketFrame =>
        CloseMessage(Some(nettyCloseFrame.getStatusCode).filter(_ > 0), nettyCloseFrame.getReasonText)
    }

    val enumerator = new WebSocketEnumerator

    (enumerator,
      new SimpleChannelUpstreamHandler {

        type FrameCreator = ChannelBuffer => WebSocketFrame

        private var continuationBuffer: Option[(FrameCreator, ChannelBuffer)] = None
        private var sentClose = false

        override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

          // Note, protocol violations like mixed up fragmentation are already handled upstream by the netty decoder
          (e.getMessage, continuationBuffer) match {

            // message too long
            case (frame: ContinuationWebSocketFrame, Some((_, buffer))) if frame.getBinaryData.readableBytes() + buffer.readableBytes() > bufferLimit =>
              closeWebSocket(ctx, WebSocketMessageTooLong, "Fragmented message too long, configured limit is " + bufferLimit)

            // non final continuation
            case (frame: ContinuationWebSocketFrame, Some((_, buffer))) if !frame.isFinalFragment =>
              buffer.writeBytes(frame.getBinaryData)

            // final continuation
            case (frame: ContinuationWebSocketFrame, Some((creator, buffer))) =>
              buffer.writeBytes(frame.getBinaryData)
              continuationBuffer = None
              val finalFrame = creator(buffer)
              enumerator.frameReceived(ctx, El(fromNettyFrame(finalFrame)))

            // fragmented text
            case (frame: TextWebSocketFrame, None) if !frame.isFinalFragment =>
              val buffer = ChannelBuffers.dynamicBuffer(Math.min(frame.getBinaryData.readableBytes() * 2, bufferLimit.asInstanceOf[Int]))
              buffer.writeBytes(frame.getBinaryData)
              continuationBuffer = Some((b => new TextWebSocketFrame(true, frame.getRsv, buffer), buffer))

            // fragmented binary
            case (frame: BinaryWebSocketFrame, None) if !frame.isFinalFragment =>
              val buffer = ChannelBuffers.dynamicBuffer(Math.min(frame.getBinaryData.readableBytes() * 2, bufferLimit.asInstanceOf[Int]))
              buffer.writeBytes(frame.getBinaryData)
              continuationBuffer = Some((b => new BinaryWebSocketFrame(true, frame.getRsv, buffer), buffer))

            case (close: CloseWebSocketFrame, None) if sentClose =>
              e.getChannel.close()

            // full handleable frame
            case (frame: WebSocketFrame, None) =>
              enumerator.frameReceived(ctx, El(fromNettyFrame(frame)))

            // unacceptable frame
            case (frame: WebSocketFrame, _) =>
              closeWebSocket(ctx, WebSocketUnacceptable, "This WebSocket does not handle frames of that type")

            case _ => //
          }
        }

        override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
          e.getCause match {
            case e: IOException =>
              logger.trace("IO exception in WebSocket", e)
            case other =>
              logger.error("Exception caught while processing WebSocket", other)
          }
          e.getChannel.close()
        }

        override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
          enumerator.frameReceived(ctx, EOF)
          logger.trace("disconnected socket")
        }

        private def closeWebSocket(ctx: ChannelHandlerContext, status: Int, reason: String): Unit = {
          if (!reason.isEmpty) {
            logger.trace("Closing WebSocket because " + reason)
          }
          if (ctx.getChannel.isOpen) {
            for {
              _ <- ctx.getChannel.write(new CloseWebSocketFrame(status, reason)).toScala
            } yield {
              enumerator.frameReceived(ctx, EOF)
            }
          }
        }

      })

  }

  private class WebSocketEnumerator extends Enumerator[Message] {

    val eventuallyIteratee = Promise[Iteratee[Message, Any]]()

    val iterateeRef = Ref[Iteratee[Message, Any]](Iteratee.flatten(eventuallyIteratee.future))

    private val promise: scala.concurrent.Promise[Iteratee[Message, Any]] = Promise[Iteratee[Message, Any]]()

    /**
     * The number of in flight messages.  Incremented every time we receive a message, decremented every time a
     * message is finished being handled.
     */
    private val inFlight = new AtomicInteger(0)

    def apply[R](i: Iteratee[Message, R]) = {
      eventuallyIteratee.success(i)
      promise.asInstanceOf[scala.concurrent.Promise[Iteratee[Message, R]]].future
    }

    def setReadable(channel: Channel, readable: Boolean) {
      if (channel.isOpen) {
        channel.setReadable(readable)
      }
    }

    def frameReceived(ctx: ChannelHandlerContext, input: Input[Message]) {
      val channel = ctx.getChannel

      if (inFlight.incrementAndGet() >= MaxInFlight) {
        setReadable(channel, false)
      }

      val eventuallyNext = Promise[Iteratee[Message, Any]]()
      val current = iterateeRef.single.swap(Iteratee.flatten(eventuallyNext.future))
      val next = current.flatFold(
        (a, e) => {
          setReadable(channel, true)
          Future.successful(current)
        },
        k => {
          if (inFlight.decrementAndGet() < MaxInFlight) {
            setReadable(channel, true)
          }
          val next = k(input)
          next.fold {
            case Step.Done(a, e) =>
              promise.success(next)
              if (channel.isOpen) {
                for {
                  _ <- channel.close().toScala
                } yield next
              } else {
                Future.successful(next)
              }

            case Step.Cont(_) =>
              Future.successful(next)
            case Step.Error(msg, e) =>
              /* deal with error, maybe close the socket */
              Future.successful(next)
          }
        },
        (err, e) => {
          setReadable(channel, true)
          /* handle error, maybe close the socket */
          Future.successful(current)
        })
      eventuallyNext.success(next)
    }
  }

  private def channelBufferToArray(buffer: ChannelBuffer) = {
    if (buffer.readableBytes() == buffer.capacity()) {
      // Use entire backing array
      buffer.array()
    } else {
      // Copy relevant bytes only
      val bytes = new Array[Byte](buffer.readableBytes())
      buffer.readBytes(bytes)
      bytes
    }
  }

  def websocketHandshake[A](ctx: ChannelHandlerContext, req: HttpRequest, e: MessageEvent, bufferLimit: Long): Enumerator[Message] = {

    val (enumerator, handler) = newWebSocketInHandler(bufferLimit)
    val p: ChannelPipeline = ctx.getChannel.getPipeline
    p.replace("handler", "handler", handler)

    WebSocketHandshake.shake(ctx, req, bufferLimit)
    enumerator
  }

  def websocketable(req: HttpRequest) = new server.WebSocketable {
    def check = HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(req.headers().get(HttpHeaders.Names.UPGRADE))
    def getHeader(header: String) = req.headers().get(header)
  }

}

object WebSocketHandler {
  private val logger = Logger(classOf[WebSocketHandler])
}
