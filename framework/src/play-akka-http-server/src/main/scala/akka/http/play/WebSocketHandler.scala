/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.http.play

import akka.http.impl.engine.ws._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.util.ByteString
import play.api.http.websocket._
import play.api.libs.streams.AkkaStreams
import play.core.server.common.WebSocketFlowHandler
import play.core.server.common.WebSocketFlowHandler.{ MessageType, RawMessage }

object WebSocketHandler {

  /**
   * Handle a WebSocket without selecting a subprotocol
   *
   * This may cause problems with clients that propose subprotocols in the
   * upgrade request and expect the server to pick one, such as Chrome.
   *
   * See https://github.com/playframework/playframework/issues/7895
   */
  @deprecated("Please specify the subprotocol (or be explicit that you specif None)", "2.7.0")
  def handleWebSocket(upgrade: UpgradeToWebSocket, flow: Flow[Message, Message, _], bufferLimit: Int): HttpResponse =
    handleWebSocket(upgrade, flow, bufferLimit, None)

  /**
   * Handle a WebSocket
   */
  def handleWebSocket(upgrade: UpgradeToWebSocket, flow: Flow[Message, Message, _], bufferLimit: Int, subprotocol: Option[String]): HttpResponse = upgrade match {
    case lowLevel: UpgradeToWebSocketLowLevel =>
      lowLevel.handleFrames(messageFlowToFrameFlow(flow, bufferLimit), subprotocol)
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
      .via(aggregateFrames(bufferLimit))
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
  private def aggregateFrames(bufferLimit: Int): GraphStage[FlowShape[FrameEvent, Either[Message, RawMessage]]] = {
    new GraphStage[FlowShape[FrameEvent, Either[Message, RawMessage]]] {

      val in = Inlet[FrameEvent]("WebSocketHandler.aggregateFrames.in")
      val out = Outlet[Either[Message, RawMessage]]("WebSocketHandler.aggregateFrames.out")

      override val shape = FlowShape.of(in, out)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {

        var currentFrameData: ByteString = null
        var currentFrameHeader: FrameHeader = null

        override def onPush(): Unit = {
          val elem = grab(in)
          elem match {
            // FrameData error handling first
            case unexpectedData: FrameData if currentFrameHeader == null =>
              // Technically impossible, this indicates a bug in Akka HTTP,
              // since it has sent the start of a frame before finishing
              // the previous frame.
              push(out, close(Protocol.CloseCodes.UnexpectedCondition, "Server error"))
            case FrameData(data, _) if currentFrameData.size + data.size > bufferLimit =>
              push(out, close(Protocol.CloseCodes.TooBig))

            // FrameData handling
            case FrameData(data, false) =>
              currentFrameData ++= data
              pull(in)
            case FrameData(data, true) =>
              val message = frameToRawMessage(currentFrameHeader, currentFrameData ++ data)
              currentFrameHeader = null
              currentFrameData = null
              push(out, Right(message))

            // Frame start error handling
            case FrameStart(header, data) if currentFrameHeader != null =>
              // Technically impossible, this indicates a bug in Akka HTTP,
              // since it has sent the start of a frame before finishing
              // the previous frame.
              push(out, close(Protocol.CloseCodes.UnexpectedCondition, "Server error"))

            // Frame start protocol errors
            case FrameStart(header, _) if header.mask.isEmpty =>
              push(out, close(Protocol.CloseCodes.ProtocolError, "Unmasked client frame"))

            // Frame start
            case fs @ FrameStart(header, data) if fs.lastPart =>
              push(out, Right(frameToRawMessage(header, data)))

            case FrameStart(header, data) =>
              currentFrameHeader = header
              currentFrameData = data
              pull(in)
          }
        }

        override def onPull(): Unit = pull(in)

        setHandlers(in, out, this)
      }
    }
  }

  private def frameToRawMessage(header: FrameHeader, data: ByteString) = {
    val unmasked = FrameEventParser.mask(data, header.mask)
    RawMessage(
      frameOpCodeToMessageType(header.opcode),
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
  private def handleProtocolFailures: Flow[WebSocketFlowHandler.RawMessage, Message, _] => Flow[Either[Message, RawMessage], Message, _] = {
    AkkaStreams.bypassWith(Flow[Either[Message, RawMessage]].via(
      new GraphStage[FlowShape[Either[Message, RawMessage], Either[RawMessage, Message]]] {

        val in = Inlet[Either[Message, RawMessage]]("WebSocketHandler.handleProtocolFailures.in")
        val out = Outlet[Either[RawMessage, Message]]("WebSocketHandler.handleProtocolFailures.out")

        override val shape = FlowShape.of(in, out)

        override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {
          var closing = false

          override def onPush(): Unit = {
            val elem = grab(in)
            elem match {
              case _ if closing =>
                completeStage()
              case Right(message) =>
                push(out, Left(message))
              case Left(close) =>
                closing = true
                push(out, Right(close))
            }
          }

          override def onPull(): Unit = pull(in)

          setHandlers(in, out, this)

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
