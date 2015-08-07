package play.core.server.common

import java.util.concurrent.atomic.AtomicReference

import akka.stream.AbruptTerminationException
import akka.stream.scaladsl._
import akka.stream.stage._
import play.api.Logger
import play.api.libs.streams.AkkaStreams
import AkkaStreams.OnlyFirstCanFinishMerge
import play.api.http.websocket._

object WebSocketFlowHandler {

  /**
   * Implements the WebSocket protocol, including correctly handling the closing of the WebSocket, as well as
   * other control frames like ping/pong.
   */
  def webSocketProtocol(flow: Flow[Message, Message, _]): Flow[Message, Message, Unit] = {
    Flow() { implicit builder =>
      import FlowGraph.Implicits._

      /**
       * This is used to track whether the client or the server initiated the close.
       */
      val state = new AtomicReference[State](Open)

      /**
       * Handles incoming control messages, specifically ping and close, and responds to them if necessary,
       * otherwise ignores them.
       *
       * This is the only place that will close the connection, either by responding to client initiated closed
       * by acking the close message and then terminating, or be receiving a client close ack, and then closing.
       *
       * In either case, the connection can only be closed after a client close message has been seen, which is
       * why this is the only place that actually closes connections.
       */
      val handleClientControlMessages = Flow[Message].transform(() => new PushStage[Message, Message] {
        def onPush(elem: Message, ctx: Context[Message]) = {
          elem match {
            case PingMessage(data) =>
              ctx.push(PongMessage(data))
            // If we receive a close message from the client, we must send it back before closing
            case close: CloseMessage if state.compareAndSet(Open, ClientInitiatedClose) =>
              ctx.pushAndFinish(close)
            // Otherwise, this must be a clients reply to a server initiated close, we can now close
            // the TCP connection
            case close: CloseMessage =>
              ctx.finish()

            case other => ctx.pull()
          }
        }
      })

      /**
       * Handles server initiated close.
       *
       * The events that can trigger a server initiated close include terminating the stream, failing, or manually
       * sending a close message.
       *
       * This stage will send finish after sending a close message
       */
      val handleServerInitiatedClose = new PushPullStage[Message, Message] {
        var closeToSend: CloseMessage = null

        def onPush(elem: Message, ctx: Context[Message]) = {
          elem match {
            case close: CloseMessage if state.compareAndSet(Open, ServerInitiatedClose) =>
              ctx.pushAndFinish(close)
            case other => ctx.push(other)
          }
        }

        def onPull(ctx: Context[Message]) = {
          if (closeToSend != null) {
            val toSend = closeToSend
            closeToSend = null
            ctx.pushAndFinish(toSend)
          } else {
            ctx.pull()
          }
        }

        override def onUpstreamFinish(ctx: Context[Message]) = {
          if (state.compareAndSet(Open, ServerInitiatedClose)) {
            closeToSend = CloseMessage(Some(CloseCodes.Regular))
            ctx.absorbTermination()
          } else {
            // Just finish, we must already be finishing.
            ctx.finish()
          }
        }

        override def onUpstreamFailure(cause: Throwable, ctx: Context[Message]) = {
          if (state.compareAndSet(Open, ServerInitiatedClose)) {
            cause match {
              case WebSocketCloseException(close) =>
                closeToSend = close
              case ignore: AbruptTerminationException =>
                // Since the flow handling the WebSocket is usually a disconnected sink/source, if the sink
                // cancels, then the source will generally never terminate. Eventually when Akka shuts the
                // actor handling it down, it will fail with an abrupt termination exception. This can generally
                // be ignored, but we handle it just in case. The closeToSend will never be sent in this
                // situation, since the Actor is shutting down.
                logger.trace("WebSocket flow did not complete its downstream, this is probably ok", ignore)
                closeToSend = CloseMessage(Some(CloseCodes.UnexpectedCondition))
              case other =>
                logger.warn("WebSocket flow threw exception, closing WebSocket", other)
                closeToSend = CloseMessage(Some(CloseCodes.UnexpectedCondition))
            }
            ctx.absorbTermination()
          } else {
            // Just fail, we must already be finishing.
            ctx.fail(cause)
          }
        }
      }

      val serverCancellationState = new AtomicReference[Either[AsyncCallback[Message], Message]](null)

      val handleServerCancellation = AkkaStreams.blockCancel[Message] { () =>
        if (state.compareAndSet(Open, ServerInitiatedClose)) {
          val close = CloseMessage(Some(CloseCodes.Regular))
          if (!serverCancellationState.compareAndSet(null, Right(close))) {
            val Left(callback) = serverCancellationState.get()
            callback.invoke(close)
          }
        }
      }

      val propagateServerCancellation = Flow[Message].transform(() => new AsyncStage[Message, Message, Message] {
        def onAsyncInput(event: Message, ctx: AsyncContext[Message, Message]) = {
          ctx.pushAndFinish(event)
        }
        def onPush(elem: Message, ctx: AsyncContext[Message, Message]) = ctx.holdUpstream()
        def onPull(ctx: AsyncContext[Message, Message]) = {
          if (!serverCancellationState.compareAndSet(null, Left(ctx.getAsyncCallback()))) {
            val Right(closeToSend) = serverCancellationState.get()
            ctx.pushAndFinish(closeToSend)
          } else {
            ctx.holdDownstream()
          }
        }
      })

      /**
       * Blocks all messages after a close message has been sent, in accordance with the WebSocket spec.
       */
      val blockAllMessagesAfterClose: Flow[Message, Message, _] = Flow[Message].transform(() => new PushPullStage[Message, Message] {
        var closeSeen = false
        def onPush(elem: Message, ctx: Context[Message]) = {
          if (closeSeen) {
            ctx.pull()
          } else {
            if (elem.isInstanceOf[CloseMessage]) {
              closeSeen = true
            }
            ctx.push(elem)
          }
        }

        def onPull(ctx: Context[Message]) = {
          ctx.pull()
        }
      })

      val broadcast = builder.add(Broadcast[Message](2))
      val merge = builder.add(OnlyFirstCanFinishMerge[Message](3))

      broadcast.out(0) ~> handleClientControlMessages ~> merge.in(0)
      broadcast.out(1) ~> handleServerCancellation ~> flow.transform(() => handleServerInitiatedClose) ~> merge.in(1)
      Source.lazyEmpty ~> propagateServerCancellation ~> merge.in(2)

      (broadcast.in, (merge.out ~> blockAllMessagesAfterClose).outlet)
    }

  }

  private sealed trait State
  private case object Open extends State
  private case object ServerInitiatedClose extends State
  private case object ClientInitiatedClose extends State

  private val logger = Logger("play.core.server.common.WebSocketFlowHandler")
}
