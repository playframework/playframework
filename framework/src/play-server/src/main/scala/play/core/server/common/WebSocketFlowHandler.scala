/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString
import play.api.Logger
import play.api.http.websocket._

object WebSocketFlowHandler {

  /**
   * Implements the WebSocket protocol, including correctly handling the closing of the WebSocket, as well as
   * other control frames like ping/pong.
   */
  def webSocketProtocol(bufferLimit: Int): BidiFlow[RawMessage, Message, Message, Message, NotUsed] = {
    BidiFlow.fromGraph(new GraphStage[BidiShape[RawMessage, Message, Message, Message]] {
      // The stream of incoming messages from the websocket connection
      val remoteIn = Inlet[RawMessage]("WebSocketIn")
      // The stream of websocket messages going to the application
      val appOut = Outlet[Message]("WebSocketAppOut")
      // The stream of websocket messages being produced by the application
      val appIn = Inlet[Message]("WebSocketAppIn")
      // The stream of websocket messages going out to the websocket connection
      val remoteOut = Outlet[Message]("WebSocketOut")

      override def shape: BidiShape[RawMessage, Message, Message, Message] = new BidiShape(remoteIn, appOut, appIn, remoteOut)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

        var state: State = Open
        var pongToSend: Message = null
        var messageToSend: Message = null

        var currentPartialMessage: RawMessage = null

        // For the remoteIn, we always and only pull when the appOut is available, the only exception being when appOut
        // is already closed and we're expecting a close ack from the client. This means whenever remoteIn pushes, we
        // always know we can push directly to appOut.  It does mean however that we will never respond to close or
        // pings if appOut never pulls.

        // For the remoteOut, we have a few buffers - a server or client initiated close buffer, a server message, and a pong
        // message.  Multiple ping messages could arrive at any time, according to the WebSocket spec, we only need to
        // respond to the most recent one, so pong messages just overwrite each other.

        // There can only ever be one server message to send, since we only ever pull if there's none to send.

        // A client initiated close message can overtake all other messages, if the client wants to close, we just send
        // it back and it misses anything that we had buffered.
        // Server messages are then treated with the next highest priority, they will be sent even if the state is
        // server initiated close.  Note that no additional server messages can be received once the state has gone into
        // server initiated close, since this is either triggered by the appIn closing, or, when appOut cancels, we
        // cancel appIn. So server messages cannot starve server initiated close from being sent.
        // The lowest priority is pong messages.

        def serverInitiatedClose(close: CloseMessage) = {
          // Cancel appIn, because we must not send any more messages once we initiate a close.
          cancel(appIn)

          if (state == Open || state.isInstanceOf[ServerInitiatingClose]) {
            if (isAvailable(remoteOut)) {
              state = ServerInitiatedClose
              push(remoteOut, close)
              // If appOut is closed, then we may need to do our own pull so that we can get the ack
              if (isClosed(appOut) && !isClosed(remoteIn) && !hasBeenPulled(remoteIn)) {
                pull(remoteIn)
              }
            } else {
              state = ServerInitiatingClose(close)
            }
          } else {
            // Initiating close when we've already sent a close message means we must have encountered an error in
            // processing the handshake, just complete.
            completeStage()
          }

        }

        def toMessage(messageType: MessageType.Type, data: ByteString): Message = {
          messageType match {
            case MessageType.Text => TextMessage(data.utf8String)
            case MessageType.Binary => BinaryMessage(data)
            case MessageType.Ping => PingMessage(data)
            case MessageType.Pong => PongMessage(data)
            case MessageType.Close => parseCloseMessage(data)
          }
        }

        def consumeMessage(): Message = {
          val read = grab(remoteIn)

          read.messageType match {
            case MessageType.Continuation if currentPartialMessage == null =>
              serverInitiatedClose(CloseMessage(CloseCodes.ProtocolError, "Unexpected continuation frame"))
              null
            case MessageType.Continuation if currentPartialMessage.data.size + read.data.size > bufferLimit =>
              serverInitiatedClose(CloseMessage(CloseCodes.TooBig, "Message was too big"))
              null
            case MessageType.Continuation if read.isFinal =>
              val message = toMessage(currentPartialMessage.messageType, currentPartialMessage.data ++ read.data)
              currentPartialMessage = null
              message
            case MessageType.Continuation =>
              currentPartialMessage = RawMessage(currentPartialMessage.messageType, currentPartialMessage.data ++ read.data, false)
              null
            case _ if currentPartialMessage != null =>
              serverInitiatedClose(CloseMessage(CloseCodes.ProtocolError, "Received non continuation frame when previous message wasn't finished"))
              null
            case _ if read.isFinal =>
              toMessage(read.messageType, read.data)
            case start =>
              currentPartialMessage = read
              null
          }
        }

        setHandler(appOut, new OutHandler {
          override def onPull() = {
            // We always pull from the remote in when the app pulls, even if closing, since if we get a message from
            // the client and we're still open, we still want to send it.
            if (!hasBeenPulled(remoteIn)) {
              pull(remoteIn)
            }
          }

          override def onDownstreamFinish() = {
            if (state == Open) {
              serverInitiatedClose(CloseMessage(Some(CloseCodes.Regular)))
            }
          }
        })

        setHandler(remoteIn, new InHandler {
          override def onPush() = {
            val message = consumeMessage()

            if (message != null) {
              state match {
                case ClientInitiatedClose(_) =>
                  // Client illegally sent a message after sending a close, just terminate
                  completeStage()
                case ServerInitiatedClose | ServerInitiatingClose(_) =>
                  // Server has initiated the close, if this is a close ack from the client, close the connection,
                  // otherwise, forward it down to the appIn if it's still listening
                  message match {
                    case close: CloseMessage =>
                      completeStage()
                    case other =>
                      if (!isClosed(appOut)) {
                        push(appOut, other)
                      } else {
                        // appIn is closed, we're ignoring the message and it's not going to pull, so we need to pull
                        pull(remoteIn)
                      }
                  }
                case Open =>
                  message match {
                    case ping @ PingMessage(data) =>
                      // Forward down to app
                      push(appOut, ping)
                      // Return to client
                      if (isAvailable(remoteOut)) {
                        // Send immediately
                        push(remoteOut, PongMessage(data))
                      } else {
                        // Store to send later
                        pongToSend = PongMessage(data)
                      }

                    case close: CloseMessage =>
                      // Forward down to app
                      push(appOut, close)
                      // And complete both app out and app in
                      complete(appOut)
                      cancel(appIn)

                      // This is a client initiated close, so send back
                      if (isAvailable(remoteOut)) {
                        // We can send, send immediately then terminate the connection
                        push(remoteOut, close)
                        completeStage()
                      } else {
                        // Store so we can send later
                        state = ClientInitiatedClose(close)
                      }

                    case other =>
                      // Forward down to app
                      push(appOut, other)

                  }
              }
            } else {
              if (!isClosed(remoteIn)) {
                pull(remoteIn)
              }
            }
          }
        })

        setHandler(appIn, new InHandler {
          override def onPush() = {
            if (state == Open) {
              grab(appIn) match {
                case close: CloseMessage =>
                  serverInitiatedClose(close)
                  cancel(appIn)
                case other =>
                  if (isAvailable(remoteOut)) {
                    push(remoteOut, other)
                  } else {
                    messageToSend = other
                  }
              }
            } else {
              // We're closed, ignore
            }
          }

          override def onUpstreamFinish() = {
            if (state == Open) {
              serverInitiatedClose(CloseMessage(Some(CloseCodes.Regular)))
            }
          }

          override def onUpstreamFailure(ex: Throwable) = {
            if (state == Open) {
              serverInitiatedClose(CloseMessage(Some(CloseCodes.UnexpectedCondition)))
              logger.error("WebSocket flow threw exception", ex)
            } else {
              logger.debug("WebSocket flow threw exception after the WebSocket was closed", ex)
            }
          }

        })

        setHandler(remoteOut, new OutHandler {
          override def onPull() = {
            state match {
              case ClientInitiatedClose(close) =>
                // Acknowledge the client close, and then complete
                push(remoteOut, close)
                completeStage()
              case ServerInitiatingClose(close) =>
                // If there is a buffered message, send that first
                if (messageToSend != null) {
                  push(remoteOut, messageToSend)
                  messageToSend = null
                } else {
                  serverInitiatedClose(close)
                }
              case ServerInitiatedClose =>
              // Ignore, we've sent a close message, we're not allowed to send anything else
              case Open =>
                if (messageToSend != null) {
                  // We have a message stored up that we didn't manage to send before, send it
                  push(remoteOut, messageToSend)
                  messageToSend = null
                } else if (pongToSend != null) {
                  // We have a pong to send
                  push(remoteOut, pongToSend)
                  pongToSend = null
                } else {
                  // Nothing to send, pull from app if not already pulled
                  if (!hasBeenPulled(appIn)) {
                    pull(appIn)
                  }
                }
            }
          }
        })

      }

    })
  }

  private sealed trait State
  private case object Open extends State
  private case class ServerInitiatingClose(message: CloseMessage) extends State
  private case object ServerInitiatedClose extends State
  private case class ClientInitiatedClose(message: CloseMessage) extends State

  private val logger = Logger("play.core.server.common.WebSocketFlowHandler")

  // Low level API for raw, possibly fragmented messages
  case class RawMessage(messageType: MessageType.Type, data: ByteString, isFinal: Boolean)
  object MessageType extends Enumeration {
    type Type = Value
    val Ping, Pong, Text, Binary, Continuation, Close = Value
  }

  def parseCloseMessage(data: ByteString): CloseMessage = {
    def invalid(reason: String) = CloseMessage(Some(CloseCodes.ProtocolError), s"Peer sent illegal close frame ($reason).")

    if (data.length >= 2) {
      val code = ((data(0) & 0xff) << 8) | (data(1) & 0xff)
      val message = data.drop(2).utf8String
      CloseMessage(Some(code), message)
    } else if (data.length == 1) {
      invalid("close code must be length 2 but was 1")
    } else {
      CloseMessage()
    }
  }

}
