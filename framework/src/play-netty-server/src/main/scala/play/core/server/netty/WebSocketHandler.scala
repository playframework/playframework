/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import akka.stream.scaladsl.Flow
import akka.stream.stage.{ Stage, PushStage, Context }
import akka.util.ByteString
import io.netty.buffer.{ Unpooled, ByteBuf, ByteBufHolder }
import io.netty.handler.codec.http.websocketx._
import io.netty.util.ReferenceCountUtil
import play.api.http.websocket.Message
import play.api.libs.streams.AkkaStreams
import play.core.server.common.WebSocketFlowHandler

import play.api.http.websocket._

private[server] object WebSocketHandler {

  /**
   * Convert a flow of messages to a flow of frame events.
   *
   * This implements the WebSocket control logic, including handling ping frames and closing the connection in a spec
   * compliant manner.
   */
  def messageFlowToFrameFlow(flow: Flow[Message, Message, _], bufferLimit: Int): Flow[WebSocketFrame, WebSocketFrame, _] = {
    // Each of the stages here transforms frames to an Either[Message, ?], where Message is a close message indicating
    // some sort of protocol failure. The handleProtocolFailures function then ensures that these messages skip the
    // flow that we are wrapping, are sent to the client and the close procedure is implemented.
    Flow[WebSocketFrame]
      .transform(() => framesToMessages(bufferLimit))
      .via(handleProtocolFailures(WebSocketFlowHandler.webSocketProtocol(flow)))
      .map(messageToFrame)
  }

  /**
   * The WebSocket protocol allows messages to be fragmented across multiple frames.
   *
   * This stage aggregates them so each frame is a full message.
   *
   * @param bufferLimit The maximum size of frame data that should be buffered.
   */
  private def framesToMessages(bufferLimit: Int): Stage[WebSocketFrame, Either[Message, Message]] = {

    new PushStage[WebSocketFrame, Either[Message, Message]] {

      var currentMessageData: ByteString = null
      var currentMessageHeader: WebSocketFrame = null

      def toMessage(frame: WebSocketFrame, data: ByteString) = frame match {
        case _: TextWebSocketFrame => TextMessage(data.utf8String)
        case _: BinaryWebSocketFrame => BinaryMessage(data)
        case close: CloseWebSocketFrame => CloseMessage(Some(close.statusCode()), close.reasonText())
        case _: PingWebSocketFrame => PingMessage(data)
        case _: PongWebSocketFrame => PongMessage(data)
      }

      def toByteString(data: ByteBufHolder) = {
        val builder = ByteString.newBuilder
        data.content().readBytes(builder.asOutputStream, data.content().readableBytes())
        val bytes = builder.result()
        bytes
      }

      def onPush(elem: WebSocketFrame, ctx: Context[Either[Message, Message]]) = {
        val directive = elem match {
          case _: ContinuationWebSocketFrame if currentMessageHeader == null =>
            ctx.push(close(CloseCodes.ProtocolError, "Unexpected continuation frame"))
          case cont: ContinuationWebSocketFrame if currentMessageData.size + cont.content().readableBytes() > bufferLimit =>
            ctx.push(close(CloseCodes.TooBig))
          case cont: ContinuationWebSocketFrame if cont.isFinalFragment =>
            val message = toMessage(currentMessageHeader, currentMessageData ++ toByteString(cont))
            currentMessageHeader = null
            currentMessageData = null
            ctx.push(Right(message))
          case cont: ContinuationWebSocketFrame =>
            currentMessageData ++= toByteString(cont)
            ctx.pull()
          case _ if currentMessageData != null =>
            ctx.push(close(CloseCodes.ProtocolError, "Received non continuation frame when previous message wasn't finished"))
          case full if full.isFinalFragment =>
            ctx.push(Right(toMessage(full, toByteString(full))))
          case start =>
            currentMessageHeader = start
            currentMessageData = toByteString(start)
            ctx.pull()
        }
        ReferenceCountUtil.release(elem)
        directive
      }
    }
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

  /**
   * Handles the protocol failures by gracefully closing the connection.
   */
  private def handleProtocolFailures: Flow[Message, Message, _] => Flow[Either[Message, Message], Message, _] = {
    AkkaStreams.bypassWith(Flow[Either[Message, Message]].transform(() => new PushStage[Either[Message, Message], Either[Message, Message]] {
      var closing = false

      def onPush(elem: Either[Message, Message], ctx: Context[Either[Message, Message]]) = elem match {
        case _ if closing =>
          ctx.finish()
        case Right(message) =>
          ctx.push(Left(message))
        case Left(close) =>
          closing = true
          ctx.push(Right(close))
      }
    }), AkkaStreams.EagerFinishMerge(2))
  }

  private def close(status: Int, message: String = "") = {
    Left(new CloseMessage(Some(status), message))
  }

}