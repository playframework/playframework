/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import scala.language.reflectiveCalls

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import play.core._
import play.core.server.websocket.WebSocketHandshake
import play.api._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import scala.concurrent.{ Future, Promise }
import scala.concurrent.stm._

import play.core.Execution.Implicits.internalContext

private[server] trait WebSocketHandler {

  import NettyFuture._

  def newWebSocketInHandler[A](frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]) = {

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
                promise.success(next)
                if (ctx.getChannel.isOpen) {
                  for {
                    _ <- ctx.getChannel.write(new CloseWebSocketFrame()).toScala
                    _ <- ctx.getChannel.close().toScala
                  } yield next
                } else {
                  Future.successful(next)
                }

              case Step.Cont(_) => Future.successful(next)
              case Step.Error(msg, e) =>
                /* deal with error, maybe close the socket */
                Future.successful(next)
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
            case frame: WebSocketFrame if nettyFrameFormatter.fromFrame.isDefinedAt(frame) =>
              enumerator.frameReceived(ctx, El(nettyFrameFormatter.fromFrame(frame)))
            case frame: CloseWebSocketFrame =>
              for {
                _ <- ctx.getChannel.write(new CloseWebSocketFrame(frame.getStatusCode, "")).toScala
                _ <- ctx.getChannel.close().toScala
              } enumerator.frameReceived(ctx, EOF)
            case frame: PingWebSocketFrame =>
              ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))
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

    val (enumerator, handler) = newWebSocketInHandler(frameFormatter)
    val p: ChannelPipeline = ctx.getChannel.getPipeline
    p.replace("handler", "handler", handler)

    WebSocketHandshake.shake(ctx, req)
    enumerator
  }

  def websocketable(req: HttpRequest) = new server.WebSocketable {
    def check = HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(req.headers().get(HttpHeaders.Names.UPGRADE))
    def getHeader(header: String) = req.headers().get(header)
  }

}
