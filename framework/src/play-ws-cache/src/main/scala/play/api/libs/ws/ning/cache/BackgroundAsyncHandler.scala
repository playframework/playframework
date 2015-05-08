package play.api.libs.ws.ning.cache

import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client._
import com.typesafe.cachecontrol.ResponseCachingActions.{ DoCacheResponse, DoNotCacheResponse }
import org.slf4j.{ LoggerFactory, Logger }

import scala.concurrent.Await

/**
 * An async handler that accumulates a response and stores it to cache in the background.
 */
class BackgroundAsyncHandler[T](request: Request, val cache: NingWSCache)
    extends AsyncHandler[T]
    with NingDebug {

  private implicit val logger: Logger = BackgroundAsyncHandler.logger

  private val timeout = scala.concurrent.duration.Duration(1, "second")

  private val builder = new CacheableResponseBuilder

  private val key = CacheKey(request)

  private var maybeThrowable: Option[Throwable] = None

  @throws(classOf[Exception])
  def onBodyPartReceived(content: HttpResponseBodyPart): AsyncHandler.STATE = {
    builder.accumulate(content)
    STATE.CONTINUE
  }

  @throws(classOf[Exception])
  def onStatusReceived(status: HttpResponseStatus): AsyncHandler.STATE = {
    builder.reset()
    builder.accumulate(status)
    STATE.CONTINUE
  }

  @throws(classOf[Exception])
  def onHeadersReceived(headers: HttpResponseHeaders): AsyncHandler.STATE = {
    builder.accumulate(headers)
    STATE.CONTINUE
  }

  def onThrowable(t: Throwable): Unit = {
    maybeThrowable = Some(t)
  }

  override def onCompleted(): T = {
    val response: CacheableResponse = builder.build

    if (cache.isNotModified(response)) {
      processNotModifiedResponse(response)
    } else {
      processFullResponse(response)
    }

    response.asInstanceOf[T]
  }

  protected def processFullResponse(fullResponse: CacheableResponse): Unit = {
    logger.debug(s"processFullResponse: fullResponse = ${debug(fullResponse)}")

    cache.cachingAction(request, fullResponse) match {
      case DoNotCacheResponse(reason) =>
        logger.debug(s"onCompleted: DO NOT CACHE, because $reason")
      case DoCacheResponse(reason) =>
        logger.debug(s"isCacheable: DO CACHE, because $reason")
        val massagedResponse = cache.stripHeaders(request, fullResponse)
        cache.cacheResponse(request, massagedResponse)
    }
  }

  protected def processNotModifiedResponse(notModifiedResponse: CacheableResponse): Unit = {
    logger.trace(s"processNotModifiedResponse: notModifiedResponse = $notModifiedResponse")

    val result = Await.result(cache.get(key), timeout)
    logger.debug(s"processNotModifiedResponse: result = $result")

    // FIXME XXX Find the response which matches the secondary keys...
    result match {
      case Some(entry) =>
        val newHeaders = notModifiedResponse.getHeaders
        val freshResponse = cache.freshenResponse(newHeaders, entry.response)
        val massagedResponse = cache.stripHeaders(request, freshResponse)
        cache.cacheResponse(request, massagedResponse)
      case None =>
      // XXX FIXME what do we do if we have a 304 and there's nothing in the cache for it?
      // If we make another call and it sends us another 304 back, we can get stuck in an
      // endless loop?

    }

  }

}

object BackgroundAsyncHandler {
  private val logger: Logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.BackgroundAsyncHandler")
}

