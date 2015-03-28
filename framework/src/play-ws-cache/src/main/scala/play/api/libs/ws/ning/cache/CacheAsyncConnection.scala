package play.api.libs.ws.ning.cache

import java.util.concurrent.{ TimeUnit, Callable }

import com.ning.http.client.listenable.AbstractListenableFuture
import com.ning.http.client.{ ProgressAsyncHandler, ListenableFuture, Request, AsyncHandler }
import org.slf4j.LoggerFactory

/**
 * Calls the relevant methods on the async handler, providing it with the cached response.
 */
class AsyncCacheableConnection[T](asyncHandler: AsyncHandler[T],
  request: Request,
  response: CacheableResponse,
  future: ListenableFuture[T])
    extends Callable[T] with NingDebug {

  import AsyncCacheableConnection._

  override def call(): T = {
    // Because this is running directly against an executor service,
    // the usual uncaught exception handler will not apply, and so
    // any kind of logging must wrap EVERYTHING in an explicit try / catch
    // block.
    try {
      if (logger.isTraceEnabled) {
        logger.trace(s"call: request = ${debug(request)}, response =  ${debug(response)}")
      }
      var state = asyncHandler.onStatusReceived(response.status)

      if (state eq AsyncHandler.STATE.CONTINUE) {
        state = asyncHandler.onHeadersReceived(response.headers)
      }

      if (state eq AsyncHandler.STATE.CONTINUE) {
        import collection.JavaConverters._
        response.bodyParts.asScala.foreach { bodyPart =>
          asyncHandler.onBodyPartReceived(bodyPart)
        }
      }

      asyncHandler match {
        case progressAsyncHandler: ProgressAsyncHandler[_] =>
          progressAsyncHandler.onHeaderWriteCompleted
          progressAsyncHandler.onContentWriteCompleted
        case _ =>
      }

      val t: T = asyncHandler.onCompleted
      future.done()
      t
    } catch {
      case t: Throwable =>
        logger.error("call: ", t)
        val ex: RuntimeException = new RuntimeException
        ex.initCause(t)
        throw ex
    }
  }

  override def toString: String = {
    s"AsyncCacheableConnection(request = ${debug(request)}})"
  }
}

object AsyncCacheableConnection {
  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.AsyncCacheableConnection")
}

/**
 * A wrapper to return a ListenableFuture.
 */
class CacheFuture[T](handler: AsyncHandler[T]) extends AbstractListenableFuture[T] {

  private var innerFuture: java.util.concurrent.Future[T] = _

  def setInnerFuture(future: java.util.concurrent.Future[T]) = {
    innerFuture = future
  }

  override def done(): Unit = {
    runListeners()
  }

  override def touch(): Unit = {

  }

  //  override def getAndSetWriteBody(writeBody: Boolean): Boolean = {
  //    true
  //  }
  //
  //  override def content(v: T): Unit = {}
  //
  //  override def getAndSetWriteHeaders(writeHeader: Boolean): Boolean = {
  //    true
  //  }

  override def abort(t: Throwable): Unit = {
    innerFuture.cancel(true)
    runListeners()
  }

  override def isCancelled: Boolean = {
    innerFuture.isCancelled
  }

  override def get(): T = {
    get(1000L, java.util.concurrent.TimeUnit.MILLISECONDS)
  }

  override def get(timeout: Long, unit: TimeUnit): T = {
    innerFuture.get(timeout, unit)
  }

  override def cancel(mayInterruptIfRunning: Boolean): Boolean = {
    runListeners()
    innerFuture.cancel(mayInterruptIfRunning)
  }

  override def isDone: Boolean = innerFuture.isDone

  override def toString: String = {
    s"CacheFuture"
  }
}