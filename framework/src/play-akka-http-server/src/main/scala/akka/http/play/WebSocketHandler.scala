package akka.http.play

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.http.impl.engine.ws._
import akka.stream.scaladsl._
import akka.stream.stage.{ Context, Stage, PushStage }
import akka.util.ByteString
import play.api.http.websocket._
import play.api.libs.streams.AkkaStreams
import play.core.server.common.WebSocketFlowHandler

object WebSocketHandler {

  /**
   * Handle a WebSocket
   */
  def handleWebSocket(upgrade: UpgradeToWebsocket, flow: Flow[Message, Message, _], bufferLimit: Int): HttpResponse = upgrade match {
    case lowLevel: UpgradeToWebsocketLowLevel =>
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
      .transform(() => aggregateMessages(bufferLimit))
      .transform(() => framesToMessages())
      .via(handleProtocolFailures(WebSocketFlowHandler.webSocketProtocol(flow)))
      .map(messageToFrameEvent)
  }

  /**
   * Akka HTTP potentially splits frames into multiple frame events.
   *
   * This stage aggregates them so each frame is a full frame.
   *
   * @param bufferLimit The maximum size of frame data that should be buffered.
   */
  private def aggregateFrames(bufferLimit: Int): Stage[FrameEvent, Either[Message, Frame]] = {
    new PushStage[FrameEvent, Either[Message, Frame]] {

      var currentFrameData: ByteString = null
      var currentFrameHeader: FrameHeader = null

      def onPush(elem: FrameEvent, ctx: Context[Either[Message, Frame]]) = elem match {
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
          val frame = Frame(currentFrameHeader, currentFrameData ++ data)
          currentFrameHeader = null
          currentFrameData = null
          ctx.push(Right(frame))

        // Frame start error handling
        case FrameStart(header, data) if currentFrameHeader != null =>
          // Technically impossible, this indicates a bug in Akka HTTP,
          // since it has sent the start of a frame before finishing
          // the previous frame.
          ctx.push(close(Protocol.CloseCodes.UnexpectedCondition, "Server error"))

        // Frame start
        case fs @ FrameStart(header, data) if fs.lastPart =>
          ctx.push(Right(Frame(header, data)))

        case FrameStart(header, data) =>
          currentFrameHeader = header
          currentFrameData = data
          ctx.pull()
      }

    }
  }

  /**
   * The WebSocket protocol allows messages to be fragmented across multiple frames.
   *
   * This stage aggregates them so each frame is a full message. It also unmasks frames.
   *
   * @param bufferLimit The maximum size of frame data that should be buffered.
   */
  private def aggregateMessages(bufferLimit: Int): Stage[Either[Message, Frame], Either[Message, Frame]] = {

    new PushStage[Either[Message, Frame], Either[Message, Frame]] {

      var currentMessageData: ByteString = null
      var currentMessageHeader: FrameHeader = null

      def onPush(elem: Either[Message, Frame], ctx: Context[Either[Message, Frame]]) = elem match {
        case close @ Left(_) => ctx.push(close)
        case Right(frame) =>
          // Protocol checks
          if (frame.header.mask.isEmpty) {
            ctx.push(close(Protocol.CloseCodes.ProtocolError, "Unmasked client frame"))
          } else if (frame.header.opcode == Protocol.Opcode.Continuation) {
            if (currentMessageHeader == null) {
              ctx.push(close(Protocol.CloseCodes.ProtocolError, "Unexpected continuation frame"))
            } else if (currentMessageData.size + frame.data.size > bufferLimit) {
              ctx.push(close(Protocol.CloseCodes.TooBig))
            } else if (frame.header.fin) {
              val currentFrame = Frame(currentMessageHeader, currentMessageData ++ frame.unmaskedData)
              currentMessageHeader = null
              currentMessageData = null
              ctx.push(Right(currentFrame))
            } else {
              currentMessageData ++= frame.unmaskedData
              ctx.pull()
            }
          } else if (currentMessageHeader != null) {
            ctx.push(close(Protocol.CloseCodes.ProtocolError, "Received non continuation frame when previous message wasn't finished"))
          } else if (frame.header.fin) {
            ctx.push(Right(Frame(frame.header, frame.unmaskedData)))
          } else {
            currentMessageHeader = frame.header
            currentMessageData = frame.unmaskedData
            ctx.pull()
          }
      }
    }
  }

  /**
   * Converts frames to Play messages.
   */
  private def framesToMessages(): Stage[Either[Message, Frame], Either[Message, Message]] = new PushStage[Either[Message, Frame], Either[Message, Message]] {

    def onPush(elem: Either[Message, Frame], ctx: Context[Either[Message, Message]]) = elem match {
      case Left(close) => ctx.push(Left(close))
      case Right(frame) =>
        frame.header.opcode match {
          case Protocol.Opcode.Binary =>
            ctx.push(Right(BinaryMessage(frame.data)))
          case Protocol.Opcode.Text =>
            ctx.push(Right(TextMessage(frame.data.utf8String)))
          case Protocol.Opcode.Close =>
            val statusCode = FrameEventParser.parseCloseCode(frame.data)
            val reason = frame.data.drop(2).utf8String
            ctx.push(Right(CloseMessage(statusCode, reason)))
          case Protocol.Opcode.Ping =>
            ctx.push(Right(PingMessage(frame.data)))
          case Protocol.Opcode.Pong =>
            ctx.push(Right(PongMessage(frame.data)))
          case other =>
            ctx.push(close(Protocol.CloseCodes.PolicyViolated))
        }
    }
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

  private case class Frame(header: FrameHeader, data: ByteString) {
    def unmaskedData = FrameEventParser.mask(data, header.mask)
  }

  private def close(status: Int, message: String = "") = {
    Left(new CloseMessage(Some(status), message))
  }

}
