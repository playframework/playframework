package play.api.libs.ws.ning.cache

import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client._
import com.typesafe.cachecontrol.ResponseCachingActions.{ DoCacheResponse, DoNotCacheResponse }
import com.typesafe.cachecontrol.ResponseServeActions.{ ValidateOrTimeout, Validate }
import com.typesafe.cachecontrol._
import org.slf4j.{ Logger, LoggerFactory }
import play.api.http.HeaderNames._

import scala.concurrent.Await

/**
 * An async handler that accumulates response data to place in cache with the given key.
 */
class CacheAsyncHandler[T](request: Request,
  handler: AsyncCompletionHandler[T],
  val cache: NingWSCache,
  maybeAction: Option[ResponseServeAction])
    extends AsyncHandler[T]
    with AsyncHandlerMethods
    with TimeoutResponse
    with NingDebug {

  private implicit val logger = CacheAsyncHandler.logger

  private val builder = new CacheableResponseBuilder()

  private val requestTime = HttpDate.now

  private val key = CacheKey(request)

  private val timeout = scala.concurrent.duration.Duration(1, "second")

  private val timeoutResponse = generateTimeoutResponse(request, cache.config)

  private val invalidator = new CacheInvalidator(cache)

  /**
   * Invoked if something wrong happened inside the previous methods or when an I/O exception occurs.
   */
  override def onThrowable(t: Throwable): Unit = {
    maybeAction match {
      case Some(ValidateOrTimeout(reason)) =>
        logger.debug(s"onCompleted: returning timeout because $reason", t)

        // If no-cache or must-revalidate exist, then a
        // successful validation has to happen -- i.e. both stale AND fresh
        // cached responses may not be returned on disconnect.
        // https://tools.ietf.org/html/rfc7234#section-5.2.2.1
        // https://tools.ietf.org/html/rfc7234#section-5.2.2.2
        timeoutResponse.asInstanceOf[T]

      case other =>
        // If not, then sending a cached response on a disconnect is acceptable
        // as long as 110 and 112 warnings are sent along with it.
        // https://tools.ietf.org/html/rfc7234#section-4.2.4
        logger.debug(s"onCompleted: action = $other", t)
        processDisconnectedResponse()
    }
  }

  /**
   * Called when the status line has been processed.
   */
  override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
    builder.accumulate(responseStatus)
    handler.onStatusReceived(responseStatus)
  }

  /**
   * Called when all response’s headers has been processed.
   */
  override def onHeadersReceived(responseHeaders: HttpResponseHeaders): STATE = {
    if (!responseHeaders.getHeaders.containsKey(DATE)) {
      /*
       A recipient with a clock that receives a response message without a
       Date header field MUST record the time it was received and append a
       corresponding Date header field to the message's header section if it
       is cached or forwarded downstream.

       https://tools.ietf.org/html/rfc7231#section-7.1.1.2
      */
      val currentDate = HttpDate.format(HttpDate.now)
      responseHeaders.getHeaders.add(DATE, currentDate)
    }
    builder.accumulate(responseHeaders)
    handler.onHeadersReceived(responseHeaders)
  }

  /**
   * Body parts has been received. This method can be invoked many time depending of the response’s bytes body.
   */
  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    builder.accumulate(bodyPart)
    handler.onBodyPartReceived(bodyPart)
  }

  /**
   * onCompleted: Invoked when the full response has been read, or if the processing get aborted (more on this below).
   */
  override def onCompleted(): T = {
    if (logger.isTraceEnabled) {
      logger.trace(s"onCompleted: this = $this")
    }

    val response = builder.build
    if (logger.isDebugEnabled) {
      logger.debug(s"onCompleted: response = ${debug(response)}")
    }

    // We got a response.  First, invalidate if unsafe according to
    // https://tools.ietf.org/html/rfc7234#section-4.4
    invalidator.invalidateIfUnsafe(request, response)

    // "Handling a Validation Response"
    // https://tools.ietf.org/html/rfc7234#section-4.3.3
    if (cache.isNotModified(response)) {
      processNotModifiedResponse(response)
    } else if (cache.isError(response)) {
      //o  However, if a cache receives a 5xx (Server Error) response while
      //attempting to validate a response, it can either forward this
      //response to the requesting client, or act as if the server failed
      //to respond.  In the latter case, the cache MAY send a previously
      //stored response (see Section 4.2.4).

      maybeAction match {
        case Some(Validate(reason, staleIfError)) if staleIfError =>
          processStaleResponse(response)
        case other =>
          processFullResponse(response)
      }
    } else {
      processFullResponse(response)
    }
  }

  protected def processDisconnectedResponse(): T = {
    logger.debug(s"processDisconnectedResponse:")

    val result = Await.result(cache.get(key), timeout)
    val finalResponse = result match {
      case Some(entry) =>
        addRevalidationFailed {
          addDisconnectHeader {
            generateCachedResponse(request, requestTime, entry)
          }
        }

      case None =>
        // Nothing in cache.  Return the timeout.
        timeoutResponse
    }
    handler.onCompleted(finalResponse)
  }

  protected def processStaleResponse(response: CacheableResponse): T = {
    logger.debug(s"processCachedResponse: response = ${debug(response)}")

    val result = Await.result(cache.get(key), timeout)
    val finalResponse = result match {
      case Some(entry) =>
        addRevalidationFailed {
          generateCachedResponse(request, requestTime, entry)
        }

      case None =>
        // Nothing in cache.  Return the error.
        response
    }
    handler.onCompleted(finalResponse)
  }

  protected def processFullResponse(fullResponse: CacheableResponse): T = {
    logger.debug(s"processFullResponse: fullResponse = ${debug(fullResponse)}")

    cache.cachingAction(request, fullResponse) match {
      case DoNotCacheResponse(reason) =>
        logger.debug(s"onCompleted: DO NOT CACHE, because $reason")
      case DoCacheResponse(reason) =>
        logger.debug(s"isCacheable: DO CACHE, because $reason")
        val massagedResponse = stripHeaders(request, fullResponse)
        cacheResponse(request, massagedResponse)
    }
    handler.onCompleted(fullResponse)
  }

  protected def processNotModifiedResponse(notModifiedResponse: CacheableResponse): T = {
    logger.trace(s"processNotModifiedResponse: notModifiedResponse = $notModifiedResponse")

    val result = Await.result(cache.get(key), timeout)
    logger.debug(s"processNotModifiedResponse: result = $result")

    // FIXME XXX Find the response which matches the secondary keys...
    val fullResponse = result match {
      case Some(entry) =>
        val newHeaders = notModifiedResponse.getHeaders
        val freshResponse = freshenResponse(newHeaders, entry.response)
        val massagedResponse = stripHeaders(request, freshResponse)
        cacheResponse(request, massagedResponse)
        massagedResponse
      case None =>
        // XXX FIXME what do we do if we have a 304 and there's nothing in the cache for it?
        // If we make another call and it sends us another 304 back, we can get stuck in an
        // endless loop?
        notModifiedResponse
    }

    handler.onCompleted(fullResponse)
  }

  override def toString = {
    s"CacheAsyncHandler(key = $key, requestTime = $requestTime, builder = $builder, asyncHandler = ${debug(handler)}})"
  }

}

object CacheAsyncHandler {
  private val logger: Logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.CacheAsyncHandler")
}

