package play.core.server.netty

import scala.language.reflectiveCalls

import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.socket.nio._
import org.jboss.netty.handler.stream._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import org.jboss.netty.handler.codec.http.websocketx._
import org.jboss.netty.channel.group._
import play.core._
import play.core.server.websocket.WebSocketHandshake
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import scala.collection.JavaConverters._
import scala.concurrent.{ Future, Promise }
import scala.concurrent.stm._

import play.core.Execution.Implicits.internalContext

private[server] trait WebSocketHandler {
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
                ctx.getChannel().disconnect();
                promise.success(next);
                Play.logger.trace("cleaning for channel " + ctx.getChannel());
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
            case frame: CloseWebSocketFrame => { ctx.getChannel().disconnect(); enumerator.frameReceived(ctx, EOF) }
            case frame: WebSocketFrame => //
            case _ => //
          }
        }

        override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
          e.getCause().printStackTrace();
          e.getChannel().close();
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
    val p: ChannelPipeline = ctx.getChannel().getPipeline();
    p.replace("handler", "handler", handler);
    enumerator
  }

  def websocketable(req: HttpRequest) = new server.WebSocketable {
    def check =
      HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(req.getHeader(HttpHeaders.Names.UPGRADE))
    def getHeader(header: String) = req.getHeader(header)
  }

}
