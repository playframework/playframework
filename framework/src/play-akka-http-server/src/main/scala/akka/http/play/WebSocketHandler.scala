/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.http.play

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.impl.engine.ws._
import akka.stream.scaladsl._
import akka.stream.stage.{ Context, Stage, PushStage }
import akka.util.ByteString
import play.api.http.websocket._
import play.api.libs.streams.AkkaStreams
import play.core.server.common.WebSocketFlowHandler
import play.core.server.common.WebSocketFlowHandler.{ MessageType, RawMessage }

object WebSocketHandler {

  /**
   * Handle a WebSocket
   */
  def handleWebSocket(upgrade: UpgradeToWebSocket, flow: Flow[Message, Message, _], bufferLimit: Int): HttpResponse = upgrade match {
    case lowLevel: UpgradeToWebSocketLowLevel =>
      lowLevel.handleFrames(messageFlowToFrameFlow(flow, bufferLimit))
    case other => throw new IllegalArgumentException("UpgradeToWebsocket is not an Akka HTTP UpgradeToWebsocketLowLevel")
  }

  /**
   * Convert a flow of messages to a flow of frame events.
   *
   * This implements the WebSocket control logic, including handling ping frames and closing the connection in a spec
   * compliant manner.
   */
  def messageFlowToFrameFlow(flow: Flow[Message, Message, _], bufferLimit: Int): Flow[FrameEvent, FrameEvent, _] = {
    // Each of the stages here transforms frames to an Either[Message, ?], where Message is a close message indicating
    // some sort of protocol failure. The handleProtocolFailures function then ensures that these messages skip the
    // flow that we are wrapping, are sent to the client and the close procedure is implemented.
    Flow[FrameEvent]
      .transform(() => aggregateFrames(bufferLimit))
      .via(handleProtocolFailures(WebSocketFlowHandler.webSocketProtocol(bufferLimit).join(flow)))
      .map(messageToFrameEvent)
  }

  /**
   * Akka HTTP potentially splits frames into multiple frame events.
   *
   * This stage aggregates them so each frame is a full frame.
   *
   * @param bufferLimit The maximum size of frame data that should be buffered.
   */
  private def aggregateFrames(bufferLimit: Int): Stage[FrameEvent, Either[Message, RawMessage]] = {
    new PushStage[FrameEvent, Either[Message, RawMessage]] {

      var currentFrameData: ByteString = null
      var currentFrameHeader: FrameHeader = null

      def onPush(elem: FrameEvent, ctx: Context[Either[Message, RawMessage]]) = elem match {
        // FrameData error handling first
        case unexpectedData: FrameData if currentFrameHeader == null =>
          // Technically impossible, this indicates a bug in Akka HTTP,
          // since it has sent the start of a frame before finishing
          // the previous frame.
          ctx.push(close(Protocol.CloseCodes.UnexpectedCondition, "Server error"))
        case FrameData(data, _) if currentFrameData.size + data.size > bufferLimit =>
          ctx.push(close(Protocol.CloseCodes.TooBig))

        // FrameData handling
        case FrameData(data, false) =>
          currentFrameData ++= data
          ctx.pull()
        case FrameData(data, true) =>
          val message = frameToRawMessage(currentFrameHeader, currentFrameData ++ data)
          currentFrameHeader = null
          currentFrameData = null
          ctx.push(Right(message))

        // Frame start error handling
        case FrameStart(header, data) if currentFrameHeader != null =>
          // Technically impossible, this indicates a bug in Akka HTTP,
          // since it has sent the start of a frame before finishing
          // the previous frame.
          ctx.push(close(Protocol.CloseCodes.UnexpectedCondition, "Server error"))

        // Frame start protocol errors
        case FrameStart(header, _) if header.mask.isEmpty =>
          ctx.push(close(Protocol.CloseCodes.ProtocolError, "Unmasked client frame"))

        // Frame start
        case fs @ FrameStart(header, data) if fs.lastPart =>
          ctx.push(Right(frameToRawMessage(header, data)))

        case FrameStart(header, data) =>
          currentFrameHeader = header
          currentFrameData = data
          ctx.pull()
      }

    }
  }

  private def frameToRawMessage(header: FrameHeader, data: ByteString) = {
    val unmasked = FrameEventParser.mask(data, header.mask)
    RawMessage(frameOpCodeToMessageType(header.opcode),
      unmasked, header.fin)
  }

  /**
   * Converts frames to Play messages.
   */
  private def frameOpCodeToMessageType(opcode: Protocol.Opcode): MessageType.Type = opcode match {
    case Protocol.Opcode.Binary =>
      MessageType.Binary
    case Protocol.Opcode.Text =>
      MessageType.Text
    case Protocol.Opcode.Close =>
      MessageType.Close
    case Protocol.Opcode.Ping =>
      MessageType.Ping
    case Protocol.Opcode.Pong =>
      MessageType.Pong
    case Protocol.Opcode.Continuation =>
      MessageType.Continuation
  }

  /**
   * Converts Play messages to Akka HTTP frame events.
   */
  private def messageToFrameEvent(message: Message): FrameEvent = {
    def frameEvent(opcode: Protocol.Opcode, data: ByteString) =
      FrameEvent.fullFrame(opcode, None, data, fin = true)
    message match {
      case TextMessage(data) => frameEvent(Protocol.Opcode.Text, ByteString(data))
      case BinaryMessage(data) => frameEvent(Protocol.Opcode.Binary, data)
      case PingMessage(data) => frameEvent(Protocol.Opcode.Ping, data)
      case PongMessage(data) => frameEvent(Protocol.Opcode.Pong, data)
      case CloseMessage(Some(statusCode), reason) => FrameEvent.closeFrame(statusCode, reason)
      case CloseMessage(None, _) => frameEvent(Protocol.Opcode.Close, ByteString.empty)
    }
  }

  /**
   * Handles the protocol failures by gracefully closing the connection.
   */
  private def handleProtocolFailures: Flow[RawMessage, Message, _] => Flow[Either[Message, RawMessage], Message, _] = {
    AkkaStreams.bypassWith(Flow[Either[Message, RawMessage]].transform(() => new PushStage[Either[Message, RawMessage], Either[RawMessage, Message]] {
      var closing = false
      def onPush(elem: Either[Message, RawMessage], ctx: Context[Either[RawMessage, Message]]) = elem match {
        case _ if closing =>
          ctx.finish()
        case Right(message) =>
          ctx.push(Left(message))
        case Left(close) =>
          closing = true
          ctx.push(Right(close))
      }
    }), Merge(2, eagerComplete = true))
  }

  private case class Frame(header: FrameHeader, data: ByteString) {
    def unmaskedData = FrameEventParser.mask(data, header.mask)
  }

  private def close(status: Int, message: String = "") = {
    Left(new CloseMessage(Some(status), message))
  }

}
