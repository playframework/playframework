package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._

import scala.concurrent.Future

private[server] trait RequestBodyHandler {

  def newRequestBodyUpstreamHandler[R](firstIteratee: Iteratee[Array[Byte], Result],
    start: SimpleChannelUpstreamHandler => Unit,
    finish: => Unit): Future[Iteratee[Array[Byte], Result]] = {
    implicit val internalContext = play.core.Execution.internalContext
    import scala.concurrent.stm._
    val redeemed = Ref(false)
    var p = Promise[Iteratee[Array[Byte], Result]]()
    val MaxMessages = 10
    val MinMessages = 10
    val counter = Ref(0)

    var iteratee: Ref[Iteratee[Array[Byte], Result]] = Ref(firstIteratee)

    def pushChunk(ctx: ChannelHandlerContext, chunk: Input[Array[Byte]]) {
      if (counter.single.transformAndGet { _ + 1 } > MaxMessages && ctx.getChannel.isOpen() && !redeemed.single())
        ctx.getChannel.setReadable(false)

      val itPromise = Promise[Iteratee[Array[Byte], Result]]()
      val current = atomic { implicit txn =>
        if (!redeemed())
          Some(iteratee.single.swap(Iteratee.flatten(itPromise.future)))
        else None
      }

      current.foreach { i =>
        i.feed(chunk).flatMap(_.unflatten).extend1 {
          case Redeemed(c @ Step.Cont(k)) =>
            continue(c.it)
          case Redeemed(finished) =>
            finish(finished.it)
          case Thrown(e) =>
            if (!redeemed.single.swap(true)) {
              p.throwing(e)
              iteratee = null; p = null;
              if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
            }
            itPromise.throwing(e)
        }
      }

      def continue(it: Iteratee[Array[Byte], Result]) {
        if (counter.single.transformAndGet { _ - 1 } <= MinMessages && ctx.getChannel.isOpen())
          ctx.getChannel.setReadable(true)
        itPromise.redeem(it)
      }

      def finish(it: Iteratee[Array[Byte], Result]) {
        if (!redeemed.single.swap(true)) {
          p.redeem(it)
          iteratee = null; p = null;
          if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
        }
        itPromise.redeem(it)
      }
    }

    start(new SimpleChannelUpstreamHandler {
      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        e.getMessage match {

          case chunk: HttpChunk if !chunk.isLast() =>
            val cBuffer = chunk.getContent()
            val bytes = new Array[Byte](cBuffer.readableBytes())
            cBuffer.readBytes(bytes)
            pushChunk(ctx, El(bytes))

          case chunk: HttpChunk if chunk.isLast() => {
            pushChunk(ctx, EOF)
            finish
          }

          case unexpected => Play.logger.error("Oops, unexpected message received in NettyServer/ChunkHandler (please report this problem): " + unexpected)

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

    p.future
  }

}
