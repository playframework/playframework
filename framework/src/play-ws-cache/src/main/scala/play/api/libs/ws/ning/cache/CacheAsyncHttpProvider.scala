/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning.cache

import java.io._

import com.ning.http.client._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.Await

trait TimeoutResponse {

  def generateTimeoutResponse(request: Request, config: AsyncHttpClientConfig): CacheableResponse = {
    val uri = request.getUri
    val status = new CacheableHttpResponseStatus(uri, config, 504, "Gateway Timeout", "")
    val headers = new CacheableHttpResponseHeaders(false, new FluentCaseInsensitiveStringsMap())
    val bodyParts = java.util.Collections.emptyList[CacheableHttpResponseBodyPart]()
    CacheableResponse(status, headers, bodyParts)
  }
}

/**
 * A provider that pulls a response from the cache.
 */
class CacheAsyncHttpProvider(config: AsyncHttpClientConfig,
  httpProvider: AsyncHttpProvider,
  val cache: NingWSCache)
    extends AsyncHttpProvider
    with AsyncHandlerMethods
    with TimeoutResponse
    with NingDebug {

  import CacheAsyncHttpProvider._
  import com.typesafe.cachecontrol.ResponseSelectionActions._
  import com.typesafe.cachecontrol.ResponseServeActions._
  import com.typesafe.cachecontrol._

  private val cacheTimeout = scala.concurrent.duration.Duration(1, "second")

  @throws(classOf[IOException])
  def execute[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = {
    handler match {
      case asyncCompletionHandler: AsyncCompletionHandler[T] =>
        execute(request, asyncCompletionHandler, null)

      case other =>
        throw new IllegalStateException("not implemented!")
    }
  }

  def close(): Unit = {
    if (logger.isTraceEnabled) {
      logger.trace("close: ")
    }
  }

  @throws(classOf[IOException])
  protected def execute[T](request: Request, handler: AsyncCompletionHandler[T], future: ListenableFuture[_]): ListenableFuture[T] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"execute: request = ${debug(request)}, handler = ${debug(handler)}, future = $future")
    }

    // Ask the cache if it has anything matching the primary key...
    val key = CacheKey(request)
    val requestTime = HttpDate.now
    val entryResults = Await.result(cache.get(key), cacheTimeout).toSeq
    if (logger.isDebugEnabled) {
      logger.debug(s"execute $key: results = $entryResults")
    }

    // Selects a response out of the results -- if there is no selected response, then
    // depending on the Cache-Control header values, the response may be to timeout or forward.
    cache.selectionAction(request, entryResults) match {
      case SelectedResponse(_, index) =>
        val entry = entryResults(index)
        logger.debug(s"execute $key: selected from cache: $entry")
        serveResponse(handler, request, entry, requestTime)

      case GatewayTimeout(reason) =>
        logger.debug(s"execute $key: $reason -- timing out ")
        serveTimeout(request, handler)

      case ForwardToOrigin(reason) =>
        logger.debug(s"execute $key: $reason -- forwarding to origin server")
        httpProvider.execute(request, cacheAsyncHandler(request, handler))
    }
  }

  /**
   * Serves a future containing the response, based on the cache behavior.
   */
  protected def serveResponse[T](handler: AsyncCompletionHandler[T], request: Request, entry: CacheEntry, requestTime: DateTime): ListenableFuture[T] = {

    val key = CacheKey(request)
    val cacheRequest = cache.generateCacheRequest(request)
    val requestMethod = entry.requestMethod
    val nominatedHeaders = entry.nominatedHeaders
    val response = entry.response
    val storedResponse = cache.generateStoredResponse(response, requestMethod, nominatedHeaders)
    val responseTime = HttpDate.now
    val age = cache.calculateCurrentAge(cacheRequest, storedResponse, requestTime, responseTime)

    cache.serveAction(cacheRequest, storedResponse, age) match {
      case ServeFresh(reason) =>
        logger.debug(s"serveResponse $key: $reason -- serving fresh response")

        // Serve fresh responses from cache, without going to the origin server.
        val freshResponse = addFreshHeaders(response, age)
        executeFromCache(handler, request, freshResponse)

      case ServeStale(reason) =>
        logger.debug(s"serveResponse $key: $reason -- serving stale response found for $key")

        // Serve stale response from cache, without going to the origin sever.
        val staleResponse = addStaleHeaders(response, age)
        executeFromCache(handler, request, staleResponse)

      case ServeStaleAndValidate(reason) =>
        logger.debug(s"serveResponse $key: $reason - serving stale response and revalidating for $key")

        // XXX FIXME How does stale-while-revalidate interact with POST / unsafe methods?
        //        A cache MUST write through requests with methods that are unsafe
        //    (Section 4.2.1 of [RFC7231]) to the origin server; i.e., a cache is
        //    not allowed to generate a reply to such a request before having
        //    forwarded the request and having received a corresponding response.

        // Run a validation request in a future (which will update the cache later)...
        val validationRequest = buildValidationRequest(request, response)
        httpProvider.execute(validationRequest, backgroundAsyncHandler(validationRequest))

        // ...AND return the response from cache.
        val staleResponse = addStaleHeaders(response, age)
        executeFromCache(handler, request, staleResponse)

      case action @ Validate(reason, staleIfError) =>
        logger.debug(s"serveResponse $key: $reason -- revalidate with staleIfError = $staleIfError")

        // Stale response requires talking to the origin server first.
        // The origin server can return a 304 Not Modified with no body,
        // so the stale response could still be used... we just need to check.
        val validationRequest = buildValidationRequest(request, response)
        httpProvider.execute(validationRequest, cacheAsyncHandler(validationRequest, handler, Some(action)))

      case action @ ValidateOrTimeout(reason) =>
        logger.debug(s"serveResponse: $reason -- must revalidate and timeout on disconnect")

        // Same as validate, but if the origin server cannot be reached, return a 504 gateway
        // timeout response instead of serving a stale response.
        val validationRequest = buildValidationRequest(request, response)
        httpProvider.execute(validationRequest, cacheAsyncHandler(request, handler, Some(action)))
    }
  }

  protected def serveTimeout[T](request: Request, handler: AsyncHandler[T]): CacheFuture[T] = {
    val timeoutResponse = generateTimeoutResponse(request, config)
    executeFromCache(handler, request, timeoutResponse)
  }

  protected def executeFromCache[T](handler: AsyncHandler[T], request: Request, response: CacheableResponse) = {
    logger.trace(s"executeFromCache: handler = ${debug(handler)}, request = ${debug(request)}, response = ${debug(response)}")

    val cacheFuture = new CacheFuture[T](handler)
    val callable = new AsyncCacheableConnection[T](handler, request, response, cacheFuture)
    val f = config.executorService.submit(callable)
    cacheFuture.setInnerFuture(f)
    cacheFuture
  }

  protected def buildValidationRequest(request: Request, response: CacheableResponse): Request = {
    logger.trace(s"buildValidationRequest: ${debug(request)}, response = ${debug(response)}")
    // https://tools.ietf.org/html/rfc7234#section-4.3.1
    // https://tools.ietf.org/html/rfc7232#section-2.4

    //A client:
    //
    //o  MUST send that entity-tag in any cache validation request (using
    //  If-Match or If-None-Match) if an entity-tag has been provided by
    //the origin server.
    //
    //o  SHOULD send the Last-Modified value in non-subrange cache
    //validation requests (using If-Modified-Since) if only a
    //Last-Modified value has been provided by the origin server.
    //
    //o  MAY send the Last-Modified value in subrange cache validation
    //requests (using If-Unmodified-Since) if only a Last-Modified value
    //has been provided by an HTTP/1.0 origin server.  The user agent
    //SHOULD provide a way to disable this, in case of difficulty.
    //
    //o  SHOULD send both validators in cache validation requests if both
    //an entity-tag and a Last-Modified value have been provided by the
    //origin server.  This allows both HTTP/1.0 and HTTP/1.1 caches to
    //respond appropriately.

    composeRequest(request) { rb =>
      val headers = response.getHeaders

      // https://tools.ietf.org/html/rfc7232#section-2.2
      Option(headers.getFirstValue("Last-Modified")).map { lastModifiedDate =>
        rb.addHeader("If-Modified-Since", lastModifiedDate)
      }

      Option(headers.getFirstValue("ETag")).map { eTag =>
        rb.addHeader("If-None-Match", eTag)
      }

      rb
    }
  }

  protected def composeRequest(request: Request)(block: RequestBuilder => RequestBuilder): Request = {
    val rb = new RequestBuilder(request)
    val builder = block(rb)
    builder.build()
  }

  protected def cacheAsyncHandler[T](request: Request, handler: AsyncCompletionHandler[T], action: Option[ResponseServeAction] = None) = {
    new CacheAsyncHandler(request, handler, cache, action)
  }

  protected def backgroundAsyncHandler[T](request: Request): BackgroundAsyncHandler[T] = {
    new BackgroundAsyncHandler(request, cache)
  }

}

object CacheAsyncHttpProvider {
  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.CacheAsyncHttpProvider")
}