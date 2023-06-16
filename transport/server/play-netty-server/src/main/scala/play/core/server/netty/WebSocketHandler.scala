/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import scala.concurrent.duration.Duration

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
import play.core.server.common.WebSocketFlowHandler
import play.core.server.common.WebSocketFlowHandler.MessageType
import play.core.server.common.WebSocketFlowHandler.RawMessage

private[server] object WebSocketHandler {

  /**
   * Convert a flow of messages to a processor of frame events.
   *
   * This implements the WebSocket control logic, including handling ping frames and closing the connection in a spec
   * compliant manner.
   */
  def messageFlowToFrameProcessor(
      flow: Flow[Message, Message, _],
      bufferLimit: Int,
      wsKeepAliveMode: String,
      wsKeepAliveMaxIdle: Duration
  )(
      implicit mat: Materializer
  ): Processor[WebSocketFrame, WebSocketFrame] = {
    // The reason we use a processor is that we *must* release the buffers synchronously, since Pekko streams drops
    // messages, which will mean we can't release the ByteBufs in the messages.
    SynchronousMappedStreams.transform(
      WebSocketFlowHandler
        .webSocketProtocol(bufferLimit, wsKeepAliveMode, wsKeepAliveMaxIdle)
        .join(flow)
        .toProcessor
        .run(),
      frameToMessage,
      messageToFrame
    )
  }

  /**
   * Converts Netty frames to Play RawMessages.
   */
  private def frameToMessage(frame: WebSocketFrame): RawMessage = {
    val builder = ByteString.newBuilder
    frame.content().readBytes(builder.asOutputStream, frame.content().readableBytes())
    val bytes = builder.result()
    ReferenceCountUtil.release(frame)

    val messageType = frame match {
      case _: TextWebSocketFrame         => MessageType.Text
      case _: BinaryWebSocketFrame       => MessageType.Binary
      case close: CloseWebSocketFrame    => MessageType.Close
      case _: PingWebSocketFrame         => MessageType.Ping
      case _: PongWebSocketFrame         => MessageType.Pong
      case _: ContinuationWebSocketFrame => MessageType.Continuation
    }

    RawMessage(messageType, bytes, frame.isFinalFragment)
  }

  /**
   * Converts Play messages to Netty frames.
   */
  private def messageToFrame(message: Message): WebSocketFrame = {
    def byteStringToByteBuf(bytes: ByteString): ByteBuf = {
      if (bytes.isEmpty) {
        Unpooled.EMPTY_BUFFER
      } else {
        Unpooled.wrappedBuffer(bytes.asByteBuffer)
      }
    }

    message match {
      case TextMessage(data)                      => new TextWebSocketFrame(data)
      case BinaryMessage(data)                    => new BinaryWebSocketFrame(byteStringToByteBuf(data))
      case PingMessage(data)                      => new PingWebSocketFrame(byteStringToByteBuf(data))
      case PongMessage(data)                      => new PongWebSocketFrame(byteStringToByteBuf(data))
      case CloseMessage(Some(statusCode), reason) => new CloseWebSocketFrame(statusCode, reason)
      case CloseMessage(None, _)                  => new CloseWebSocketFrame()
    }
  }
}
