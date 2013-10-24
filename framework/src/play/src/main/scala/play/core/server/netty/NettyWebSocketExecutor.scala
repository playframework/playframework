/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import play.core.system.{ HandlerExecutorContext, HandlerExecutor }
import play.api.Play
import play.api.mvc._
import play.api.libs.iteratee._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{ HttpHeaders, HttpRequest }
import play.core.server.websocket.WebSocketHandshake
import play.core.server
import scala.concurrent.{ Future, Promise }
import org.jboss.netty.handler.codec.http.websocketx.{ CloseWebSocketFrame, WebSocketFrame }
import play.api.libs.iteratee.Input._
import scala.concurrent.stm.Ref

/**
 * WebSocket handler for the Netty backend
 */
object NettyWebSocketExecutor extends HandlerExecutor[NettyBackendRequest] {

  def apply(context: HandlerExecutorContext[NettyBackendRequest], request: RequestHeader, backend: NettyBackendRequest,
    handler: Handler) = {

    val nettyHttpRequest = backend.event.getMessage.asInstanceOf[HttpRequest]
    val websocketableRequest = websocketable(nettyHttpRequest)

    handler match {
      case ws @ WebSocket(f) if websocketableRequest.check =>
        Play.logger.trace("Serving this request with: " + ws)
        val enumerator = websocketHandshake(backend.context, nettyHttpRequest, backend.event)(ws.frameFormatter)
        f(request)(enumerator, socketOut(backend.context)(ws.frameFormatter))
        Some(NettyPromise(backend.context.getChannel.getCloseFuture))
      //handle bad websocket request
      case WebSocket(f) =>
        Play.logger.trace("Bad websocket request")
        context.sendResult(request, backend, Future.successful(Results.BadRequest))
      case _ => None
    }
  }

  def newWebSocketInHandler[A](frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]) = {

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val nettyFrameFormatter = frameFormatter.asInstanceOf[play.core.server.websocket.FrameFormatter[A]]

    val enumerator = new Enumerator[A] {

      val eventuallyIteratee = Promise[Iteratee[A, Any]]()

      val iterateeRef = Ref[Iteratee[A, Any]](Iteratee.flatten(eventuallyIteratee.future))

      private val promise: scala.concurrent.Promise[Iteratee[A, Any]] = Promise[Iteratee[A, Any]]()

      def apply[R](i: Iteratee[A, R]) = {
        eventuallyIteratee.success(i /* TODO: use a buffer enumeratee to fail when too many messages */ )
        promise.asInstanceOf[scala.concurrent.Promise[Iteratee[A, R]]].future
      }

      def frameReceived(ctx: ChannelHandlerContext, input: Input[A]) {

        val eventuallyNext = Promise[Iteratee[A, Any]]()
        val current = iterateeRef.single.swap(Iteratee.flatten(eventuallyNext.future))
        val next = current.flatFold(
          (a, e) => { sys.error("Getting messages on a supposedly closed socket? frame: " + input) },
          k => {
            val next = k(input)
            next.fold {
              case Step.Done(a, e) =>
                ctx.getChannel.disconnect()
                promise.success(next)
                Play.logger.trace("cleaning for channel " + ctx.getChannel)
                Future.successful(next)

              case Step.Cont(_) => Future.successful(next)
              case Step.Error(msg, e) => { /* deal with error, maybe close the socket */ Future.successful(next) }
            }
          },
          (err, e) => /* handle error, maybe close the socket */ Future.successful(current))
        eventuallyNext.success(next)
      }
    }

    (enumerator,
      new SimpleChannelUpstreamHandler {

        override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
          e.getMessage match {
            case frame: WebSocketFrame if nettyFrameFormatter.fromFrame.isDefinedAt(frame) => {
              enumerator.frameReceived(ctx, El(nettyFrameFormatter.fromFrame(frame)))
            }
            case frame: CloseWebSocketFrame => { ctx.getChannel.disconnect(); enumerator.frameReceived(ctx, EOF) }
            case frame: WebSocketFrame => //
            case _ => //
          }
        }

        override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
          e.getCause.printStackTrace()
          e.getChannel.close()
        }

        override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
          enumerator.frameReceived(ctx, EOF)
          Play.logger.trace("disconnected socket")
        }

      })

  }

  def websocketHandshake[A](ctx: ChannelHandlerContext, req: HttpRequest, e: MessageEvent)(frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): Enumerator[A] = {

    WebSocketHandshake.shake(ctx, req)

    val (enumerator, handler) = newWebSocketInHandler(frameFormatter)
    val p: ChannelPipeline = ctx.getChannel.getPipeline
    p.replace("handler", "handler", handler)
    enumerator
  }

  def websocketable(req: HttpRequest) = new server.WebSocketable {
    def check =
      HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(req.getHeader(HttpHeaders.Names.UPGRADE))
    def getHeader(header: String) = req.getHeader(header)
  }

  def socketOut[A](ctx: ChannelHandlerContext)(frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): Iteratee[A, Unit] = {
    val channel = ctx.getChannel
    val nettyFrameFormatter = frameFormatter.asInstanceOf[play.core.server.websocket.FrameFormatter[A]]

    def step(future: Option[ChannelFuture])(input: Input[A]): Iteratee[A, Unit] =
      input match {
        case El(e) => Cont(step(Some(channel.write(nettyFrameFormatter.toFrame(e)))))
        case e @ EOF => future.map(_.addListener(ChannelFutureListener.CLOSE)).getOrElse(channel.close()); Done((), e)
        case Empty => Cont(step(future))
      }

    import play.api.libs.iteratee.Execution.Implicits.trampoline
    Enumeratee.breakE[A](_ => !channel.isConnected).transform(Cont(step(None)))
  }

}
