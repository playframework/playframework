package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

import play.api._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._

import scala.concurrent.{ Future, Promise }
import scala.util.{ Try, Success }

private[server] trait RequestBodyHandler {

  /**
   * Creates a new upstream handler for the purposes of receiving chunked requests. Requests are buffered as an
   * optimization.
   *
   * @param bodyHandler the iteratee used to handle the body.
   * @param replaceHandler a function to handle the registration of a new handler. A handler is passed as a param.
   * @param handlerFinished a function to handle the de-registration of the handler i.e. when the chunked request is complete.
   * @return a future of an iteratee that will return the result.
   */
  def newRequestBodyUpstreamHandler[A](bodyHandler: Iteratee[Array[Byte], A],
    replaceHandler: ChannelUpstreamHandler => Unit,
    handlerFinished: => Unit): Future[A] = {

    implicit val internalContext = play.core.Execution.internalContext
    import scala.concurrent.stm._

    // A promise for the result of the body handler.  This will be returned to the caller immediately.
    val bodyHandlerResult = Promise[Iteratee[Array[Byte], A]]()

    // Constants for how many messages we're prepared to allow to be in flight.
    val MaxMessages = 10
    val MinMessages = 10

    // How messages we currently have "in flight" that the bodyHandler hasn't acknowledged.
    val counter = Ref(0)

    // An STM reference to the body handler.
    // I don't think STM is needed here, since Netty won't give us two chunks at once.
    // But I don't want to remove it, in case I've misunderstood.
    // TODO: Work out whether STM is needed.
    val iteratee: Ref[Iteratee[Array[Byte], A]] = Ref(bodyHandler)

    def pushChunk(ctx: ChannelHandlerContext, chunk: Input[Array[Byte]]) {

      // If we have more messages in flight than the maximum, ensure that we have told upstream that
      // we don't want to received more.  But only if the channel is still open and we haven't finished handling it.
      if (counter.single.transformAndGet { _ + 1 } > MaxMessages && ctx.getChannel.isOpen && !bodyHandlerResult.isCompleted)
        ctx.getChannel.setReadable(false)

      // Promise for the next iteratee
      val itPromise = Promise[Iteratee[Array[Byte], A]]()

      // Update our body handler iteratee to be the next iteratee, and get the current one atomically
      val current = atomic { implicit txn =>
        if (!bodyHandlerResult.isCompleted) {
          Some(iteratee.single.swap(Iteratee.flatten(itPromise.future)))
        } else {
          // We already have a result, but we're not at the end of the stream yet.  Replace the handler with one that
          // will simply ignore the rest of the body.
          // This means we can free up resources that this handler holds, namely, the promise for the parsed body,
          // which could be large, while the rest of the body comes.
          if (chunk != Input.EOF) {
            replaceHandler(new IgnoreBodyHandler(handlerFinished))
          }
          None
        }
      }

      current.foreach { currentIteratee =>
        // Feed the chunk
        currentIteratee.feed(chunk).flatMap(_.unflatten).onComplete {
          case Success(c @ Step.Cont(k)) =>
            continue(c.it)
          case done =>
            finish(done.map(_.it))
        }
      }

      def continue(it: Iteratee[Array[Byte], A]) {
        // If we have less messages in flight than the minimum, ensure that we have told upstream that
        // we are ready to receive more.
        if (counter.single.transformAndGet { _ - 1 } <= MinMessages && ctx.getChannel.isOpen)
          ctx.getChannel.setReadable(true)
        itPromise.success(it)
      }

      def finish(result: Try[Iteratee[Array[Byte], A]]) {
        // Redeem the body handler result
        if (!bodyHandlerResult.tryComplete(result)) {
          if (ctx.getChannel.isOpen) ctx.getChannel.setReadable(true)
        }
        itPromise.complete(result)
      }
    }

    replaceHandler(new SimpleChannelUpstreamHandler {
      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        e.getMessage match {

          case chunk: HttpChunk if !chunk.isLast =>
            val cBuffer = chunk.getContent
            val bytes = new Array[Byte](cBuffer.readableBytes())
            cBuffer.readBytes(bytes)
            pushChunk(ctx, El(bytes))

          case chunk: HttpChunk if chunk.isLast => {
            pushChunk(ctx, EOF)
            handlerFinished
          }

          case unexpected =>
            Play.logger.error("Oops, unexpected message received in NettyServer/RequestBodyHandler" +
              " (please report this problem): " + unexpected)

        }
      }

      override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        Play.logger.error("Exception caught in RequestBodyHandler", e.getCause)
        e.getChannel.close()
      }

      override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        pushChunk(ctx, EOF)
      }

    })

    bodyHandlerResult.future.flatMap(_.run)
  }

  /**
   * Ignores the body, but calls finish when finished.
   */
  private class IgnoreBodyHandler(handlerFinished: => Unit) extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      e.getMessage match {
        case chunk: HttpChunk => {
          // Ignore, and invoke the callback if it's the last chunk.
          if (chunk.isLast) handlerFinished
        }

        // Even though this handler essentially ignores everything it receives, it should only be handling HTTP chunks,
        // so if it gets something else log it so that we can know there's a bug.
        case unexpected =>
          Play.logger.error("Oops, unexpected message received in NettyServer/IgnoreBodyHandler" +
            " (please report this problem): " + unexpected)
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      Play.logger.error("Exception caught in IgnoreBodyHandler", e.getCause)
      e.getChannel.close()
    }
  }
}
