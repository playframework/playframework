package play.core.server.netty

import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.socket.nio._
import org.jboss.netty.handler.stream._

import org.jboss.netty.channel.group._
import java.util.concurrent._

import play.core._
import server.Server
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._

import scala.collection.JavaConverters._

private[server] trait RequestBodyHandler {

  def newRequestBodyHandler[R](firstIteratee: Promise[Iteratee[Array[Byte], Either[Result, R]]], allChannels: DefaultChannelGroup, server: Server): (Promise[Iteratee[Array[Byte], Either[Result, R]]], SimpleChannelUpstreamHandler) = {
    var redeemed = false
    var p = Promise[Iteratee[Array[Byte], Either[Result, R]]]()
    val MAX_MESSAGE_WATERMARK = 10
    val MIN_MESSAGE_WATERMARK = 10
    import scala.concurrent.stm._
    val counter = Ref(0)

    var iteratee: Ref[Iteratee[Array[Byte], Either[Result, R]]] = Ref(Iteratee.flatten(firstIteratee))

    def pushChunk(ctx: ChannelHandlerContext, chunk: Input[Array[Byte]]) {

      if (counter.single.transformAndGet { _ + 1 } > MAX_MESSAGE_WATERMARK && ctx.getChannel.isOpen())
        ctx.getChannel.setReadable(false)

      if (!redeemed) {
        val itPromise = Promise[Iteratee[Array[Byte], Either[Result, R]]]()
        val current = iteratee.single.swap(Iteratee.flatten(itPromise))
        val next = current.pureFlatFold[Array[Byte], Either[Result, R]](
          (_, _) => current,
          k => k(chunk),
          (e, _) => current)

        itPromise.redeem(next)

        next.pureFold(
          (a, e) => if (!redeemed) {
            p.redeem(next);
            iteratee = null; p = null; redeemed = true
            if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
          },
          k =>
            if (counter.single.transformAndGet { _ - 1 } <= MIN_MESSAGE_WATERMARK && ctx.getChannel.isOpen())
              ctx.getChannel.setReadable(true),

          (msg, e) =>
            if (!redeemed) {
              p.redeem(Done(Left(Results.InternalServerError), e))
              iteratee = null; p = null; redeemed = true
              if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
            })
      }
    }

    (p, new SimpleChannelUpstreamHandler {
      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        e.getMessage match {

          case chunk: HttpChunk if !chunk.isLast() =>
            val cBuffer = chunk.getContent()
            val bytes = new Array[Byte](cBuffer.readableBytes())
            cBuffer.readBytes(bytes)
            pushChunk(ctx, El(bytes))

          case chunk: HttpChunk if chunk.isLast() => {
            pushChunk(ctx, EOF)
            ctx.getChannel.getPipeline.replace("handler", "handler", new PlayDefaultUpstreamHandler(server, allChannels))
          }

          case unexpected => Logger("play").error("Oops, unexpected message received in NettyServer/ChunkHandler (please report this problem): " + unexpected)

        }
      }

      override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        e.getCause().printStackTrace();
        e.getChannel().close(); /*really? */
      }
      override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        pushChunk(ctx, EOF)
      }

    })

  }

}
