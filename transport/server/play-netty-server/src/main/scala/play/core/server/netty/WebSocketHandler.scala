/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.netty.buffer.{ Unpooled, ByteBuf }
import io.netty.handler.codec.http.websocketx._
import io.netty.util.ReferenceCountUtil
import org.reactivestreams.Processor
import play.api.http.websocket.Message
import play.core.server.common.WebSocketFlowHandler

import play.api.http.websocket._
import play.core.server.common.WebSocketFlowHandler.{ MessageType, RawMessage }

private[server] object WebSocketHandler {

  /**
   * Convert a flow of messages to a processor of frame events.
   *
   * This implements the WebSocket control logic, including handling ping frames and closing the connection in a spec
   * compliant manner.
   */
  def messageFlowToFrameProcessor(flow: Flow[Message, Message, _], bufferLimit: Int)(implicit mat: Materializer): Processor[WebSocketFrame, WebSocketFrame] = {

    // The reason we use a processor is that we *must* release the buffers synchronously, since Akka streams drops
    // messages, which will mean we can't release the ByteBufs in the messages.
    SynchronousMappedStreams.transform(
      WebSocketFlowHandler.webSocketProtocol(bufferLimit).join(flow).toProcessor.run(),
      frameToMessage, messageToFrame)
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
      case _: TextWebSocketFrame => MessageType.Text
      case _: BinaryWebSocketFrame => MessageType.Binary
      case close: CloseWebSocketFrame => MessageType.Close
      case _: PingWebSocketFrame => MessageType.Ping
      case _: PongWebSocketFrame => MessageType.Pong
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
      case TextMessage(data) => new TextWebSocketFrame(data)
      case BinaryMessage(data) => new BinaryWebSocketFrame(byteStringToByteBuf(data))
      case PingMessage(data) => new PingWebSocketFrame(byteStringToByteBuf(data))
      case PongMessage(data) => new PongWebSocketFrame(byteStringToByteBuf(data))
      case CloseMessage(Some(statusCode), reason) => new CloseWebSocketFrame(statusCode, reason)
      case CloseMessage(None, _) => new CloseWebSocketFrame()
    }
  }
}
