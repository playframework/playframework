/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import scala.concurrent.duration._

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.websocketx._
import io.netty.util.ReferenceCountUtil
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.reactivestreams.Processor
import play.api.http.websocket._
import play.api.http.websocket.Message
import play.api.mvc.WebSocket
import play.core.server.common.WebSocketFlowHandler
import play.core.server.common.WebSocketFlowHandler.MessageType
import play.core.server.common.WebSocketFlowHandler.RawMessage

private[server] trait PlayWebSocketCompressionFrame {
  def shouldCompress: Boolean
}

private[server] object WebSocketHandler {

  /**
   * Convert a flow of messages to a processor of frame events.
   *
   * This implements the WebSocket control logic, including handling ping frames and closing the connection in a spec
   * compliant manner.
   */
  def messageFlowToFrameProcessor(
      flow: Flow[Message, Message, ?],
      bufferLimit: Int,
      compressionThreshold: Long,
      compressionSelector: WebSocket.CompressionContext => Boolean,
      wsKeepAliveMode: String,
      wsKeepAliveMaxIdle: Duration
  )(
      implicit mat: Materializer
  ): Processor[WebSocketFrame, WebSocketFrame] =
    messageFlowToFrameProcessor(
      flow,
      bufferLimit,
      compressionThreshold,
      compressionSelector,
      wsKeepAliveMode,
      wsKeepAliveMaxIdle,
      3.seconds,
      None
    )

  def messageFlowToFrameProcessor(
      flow: Flow[Message, Message, ?],
      bufferLimit: Int,
      compressionThreshold: Long,
      compressionSelector: WebSocket.CompressionContext => Boolean,
      wsKeepAliveMode: String,
      wsKeepAliveMaxIdle: Duration,
      wsCloseTimeout: FiniteDuration
  )(
      implicit mat: Materializer
  ): Processor[WebSocketFrame, WebSocketFrame] =
    messageFlowToFrameProcessor(
      flow,
      bufferLimit,
      compressionThreshold,
      compressionSelector,
      wsKeepAliveMode,
      wsKeepAliveMaxIdle,
      wsCloseTimeout,
      None
    )

  def messageFlowToFrameProcessor(
      flow: Flow[Message, Message, ?],
      bufferLimit: Int,
      compressionThreshold: Long,
      compressionSelector: WebSocket.CompressionContext => Boolean,
      wsKeepAliveMode: String,
      wsKeepAliveMaxIdle: Duration,
      wsCloseTimeout: FiniteDuration,
      gracefulShutdown: Option[(CloseMessage => Unit) => (() => Unit)]
  )(
      implicit mat: Materializer
  ): Processor[WebSocketFrame, WebSocketFrame] = {
    // The reason we use a processor is that we *must* release the buffers synchronously, since Pekko streams drops
    // messages, which will mean we can't release the ByteBufs in the messages.
    val processor = SynchronousMappedStreams.transform(
      WebSocketFlowHandler
        .webSocketProtocol(bufferLimit, wsKeepAliveMode, wsKeepAliveMaxIdle, wsCloseTimeout, gracefulShutdown)
        .join(flow)
        .toProcessor
        .run(),
      frameToMessage,
      message => messageToFrame(message, compressionThreshold, compressionSelector)
    )

    new Processor[WebSocketFrame, WebSocketFrame] {
      override def onSubscribe(subscription: org.reactivestreams.Subscription): Unit =
        processor.onSubscribe(subscription)
      override def onNext(frame: WebSocketFrame): Unit                                              = processor.onNext(frame)
      override def onComplete(): Unit                                                               = processor.onComplete()
      override def onError(error: Throwable): Unit                                                  = processor.onError(normalizeProtocolViolation(error))
      override def subscribe(subscriber: org.reactivestreams.Subscriber[? >: WebSocketFrame]): Unit =
        processor.subscribe(subscriber)
    }
  }

  private def normalizeProtocolViolation(error: Throwable): Throwable = error match {
    case corrupted: CorruptedWebSocketFrameException =>
      new WebSocketFlowHandler.BackendHandledProtocolViolation(corrupted)
    case other => other
  }

  /**
   * Converts Netty frames to Play RawMessages.
   */
  private def frameToMessage(frame: WebSocketFrame): RawMessage = {
    val reservedBits  = frame.rsv()
    val finalFragment = frame.isFinalFragment
    val messageType   = if (reservedBits != 0) {
      frame match {
        case _: CloseWebSocketFrame => MessageType.CloseWithReservedBits
        case _                      => MessageType.ReservedBits
      }
    } else {
      frame match {
        case _: TextWebSocketFrame         => MessageType.Text
        case _: BinaryWebSocketFrame       => MessageType.Binary
        case _: CloseWebSocketFrame        => MessageType.Close
        case _: PingWebSocketFrame         => MessageType.Ping
        case _: PongWebSocketFrame         => MessageType.Pong
        case _: ContinuationWebSocketFrame => MessageType.Continuation
      }
    }
    val data =
      try {
        if (reservedBits != 0) {
          ByteString.empty
        } else {
          val builder = ByteString.newBuilder
          frame.content().readBytes(builder.asOutputStream, frame.content().readableBytes())
          builder.result()
        }
      } finally {
        ReferenceCountUtil.release(frame)
      }

    RawMessage(messageType, data, finalFragment)
  }

  /**
   * Converts Play messages to Netty frames.
   */
  private def messageToFrame(
      message: Message,
      compressionThreshold: Long,
      compressionSelector: WebSocket.CompressionContext => Boolean
  ): WebSocketFrame = {
    def byteStringToByteBuf(bytes: ByteString): ByteBuf = {
      if (bytes.isEmpty) {
        Unpooled.EMPTY_BUFFER
      } else {
        Unpooled.wrappedBuffer(bytes.asByteBuffer)
      }
    }

    def compressionContext(payloadLength: Long) =
      new WebSocket.CompressionContext(
        message,
        payloadLength,
        payloadLength > compressionThreshold
      )

    message match {
      case TextMessage(data) =>
        val bytes = ByteString(data)
        new TextWebSocketFrame(true, 0, byteStringToByteBuf(bytes)) with PlayWebSocketCompressionFrame {
          override lazy val shouldCompress: Boolean = compressionSelector(compressionContext(bytes.length.toLong))
        }
      case BinaryMessage(data) =>
        new BinaryWebSocketFrame(byteStringToByteBuf(data)) with PlayWebSocketCompressionFrame {
          override lazy val shouldCompress: Boolean = compressionSelector(compressionContext(data.length.toLong))
        }
      case PingMessage(data)                      => new PingWebSocketFrame(byteStringToByteBuf(data))
      case PongMessage(data)                      => new PongWebSocketFrame(byteStringToByteBuf(data))
      case CloseMessage(Some(statusCode), reason) => new CloseWebSocketFrame(statusCode, reason)
      case CloseMessage(None, _)                  => new CloseWebSocketFrame()
    }
  }
}
