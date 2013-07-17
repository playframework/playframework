package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

private[server] trait RequestBodyHandler {

  /**
   * Creates a new upstream handler for the purposes of receiving chunked requests. Requests are buffered as an
   * optimization.
   * @param firstIteratee the iteratee to be used to process the first chunk
   * @param start a function to handle the registration of the handler. A handler is passed as a param.
   * @param finish a function to handle the de-registration of the handler i.e. when the chunked request is complete.
   * @return a future of an iteratee that will return the result.
   */
  def newRequestBodyUpstreamHandler[A](firstIteratee: Iteratee[Array[Byte], A],
    start: SimpleChannelUpstreamHandler => Unit,
    finish: => Unit): Future[A] = {

    implicit val internalContext = play.core.Execution.internalContext
    import scala.concurrent.stm._
    var p = Promise[Iteratee[Array[Byte], A]]()
    val MaxMessages = 10
    val MinMessages = 10
    val counter = Ref(0)

    var iteratee: Ref[Iteratee[Array[Byte], A]] = Ref(firstIteratee)

    def pushChunk(ctx: ChannelHandlerContext, chunk: Input[Array[Byte]]) {
      if (counter.single.transformAndGet { _ + 1 } > MaxMessages && ctx.getChannel.isOpen() && !p.isCompleted)
        ctx.getChannel.setReadable(false)

      val itPromise = Promise[Iteratee[Array[Byte], A]]()
      val current = atomic { implicit txn =>
        if (!p.isCompleted)
          Some(iteratee.single.swap(Iteratee.flatten(itPromise.future)))
        else None
      }

      current.foreach { i =>
        i.feed(chunk).flatMap(_.unflatten).onComplete {
          case Success(c @ Step.Cont(k)) =>
            continue(c.it)
          case Success(finished) =>
            finish(finished.it)
          case Failure(e) =>
            if (!p.tryFailure(e)) {
              iteratee = null; p = null;
              if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
            }
            itPromise.failure(e)
        }
      }

      def continue(it: Iteratee[Array[Byte], A]) {
        if (counter.single.transformAndGet { _ - 1 } <= MinMessages && ctx.getChannel.isOpen())
          ctx.getChannel.setReadable(true)
        itPromise.success(it)
      }

      def finish(it: Iteratee[Array[Byte], A]) {
        if (!p.trySuccess(it)) {
          iteratee = null; p = null;
          if (ctx.getChannel.isOpen()) ctx.getChannel.setReadable(true)
        }
        itPromise.success(it)
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

    p.future.flatMap(_.run)
  }

}
