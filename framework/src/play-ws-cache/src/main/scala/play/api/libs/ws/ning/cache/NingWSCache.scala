/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning.cache

import java.net.URI

import com.google.common.cache.{ Cache => GCache, CacheBuilder => GCacheBuilder }
import com.ning.http.client._
import com.typesafe.cachecontrol._
import org.joda.time.{ DateTime, Seconds }
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/*
https://tools.ietf.org/html/rfc7234#section-2

The primary cache key consists of the request method and target URI.
 However, since HTTP caches in common use today are typically limited
 to caching responses to GET, many caches simply decline other methods
 and use only the URI as the primary cache key.
*/

/**
 * A cache entry with an optional expiry time
 */
case class CacheEntry(response: CacheableResponse,
    requestMethod: String,
    nominatedHeaders: Map[HeaderName, Seq[String]],
    expiresAt: Option[DateTime]) {

  /**
   * Has the entry expired yet?
   */
  def isExpired: Boolean = expiresAt.exists(_.isBeforeNow)
}

/**
 * Central cache used in a NingWSClient.
 */
class NingWSCache(val config: AsyncHttpClientConfig, underlying: GCache[CacheKey, CacheEntry]) extends CacheDefaults with NingDebug {

  import NingWSCache._

  private val responseCachingCalculator = new ResponseCachingCalculator(this)

  private val responseServingCalculator = new ResponseServingCalculator(this)

  private val responseSelectionCalculator = new ResponseSelectionCalculator(this)

  private val stripHeaderCalculator = new StripHeaderCalculator(this)

  private val secondaryKeyCalculator = new SecondaryKeyCalculator()

  private val currentAgeCalculator = new CurrentAgeCalculator()

  /**
   * Cache is not shared.
   */
  override def isShared: Boolean = false

  def get(key: CacheKey): Future[Option[CacheEntry]] = {
    logger.debug(s"get: key = $key")
    require(key != null, "key is null")
    val entry = Option(underlying.getIfPresent(key))
    Future.successful(entry)
  }

  def put(key: CacheKey, entry: CacheEntry): Future[Unit] = {
    logger.debug(s"put: key = $key, entry = $entry")
    require(entry != null, "value is null")

    underlying.put(key, entry)
    Future.successful(())
  }

  def remove(key: CacheKey): Future[Unit] = {
    require(key != null, "key is null")
    Future.successful(underlying.invalidate(key))
  }

  /**
   * Invalidates the key.
   */
  def invalidateKey(key: CacheKey): Unit = {
    // mark any caches as stale by replacing the date with TSE
    Option(underlying.getIfPresent(key)).foreach { entry =>
      val expiredEntry = entry.copy(expiresAt = Some(HttpDate.fromEpochSeconds(0)))
      put(key, expiredEntry)
    }
  }

  def cachingAction(request: Request, response: CacheableResponse): ResponseCachingAction = {
    val headers = response.headers
    val statusCode = response.getStatusCode
    val cacheRequest = generateCacheRequest(request)
    val originResponse: OriginResponse = generateOriginResponse(request, statusCode, headers)
    val action = responseCachingCalculator.isCacheable(cacheRequest, originResponse)
    action
  }

  def selectionAction(request: Request, entries: Seq[CacheEntry]): ResponseSelectionAction = {
    val cacheRequest = generateCacheRequest(request)
    val storedResponses = entries.map { entry =>
      generateStoredResponse(entry.response, entry.requestMethod, entry.nominatedHeaders)
    }

    responseSelectionCalculator.selectResponse(cacheRequest, storedResponses)
  }

  def serveAction(request: CacheRequest, response: StoredResponse, age: Seconds): ResponseServeAction = {
    responseServingCalculator.serveResponse(request, response, age)
  }

  override def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
    // XXX FIXME Look at LM-Freshness algorithm
    None
  }

  override def isCacheableExtension(extension: CacheDirectives.CacheDirectiveExtension): Boolean = {
    false
  }

  def isNotModified(response: CacheableResponse): Boolean = {
    response.getStatusCode == 304
  }

  def isError(response: CacheableResponse): Boolean = {
    // In this context, an error is any situation that would result in a
    // 500, 502, 503, or 504 HTTP response status code being returned.
    // https://tools.ietf.org/html/rfc5861#section-3

    response.getStatusCode match {
      case 500 | 502 | 503 | 504 =>
        true
      case other =>
        false
    }
  }

  def isUnsafeMethod(request: Request): Boolean = {
    // Of the request methods defined by this specification, the GET, HEAD,
    // OPTIONS, and TRACE methods are defined to be safe.
    // https://tools.ietf.org/html/rfc7231#section-4.2.1
    request.getMethod match {
      case "GET" | "HEAD" | "OPTIONS" | "TRACE" =>
        false
      case other =>
        true
    }
  }

  def calculateSecondaryKeys(request: Request, response: Response): Option[Map[HeaderName, Seq[String]]] = {
    val cacheRequest = generateCacheRequest(request)
    val headers = ningHeadersToMap(response.getHeaders).map {
      case (name, values) =>
        (HeaderName(name), values)
    }

    secondaryKeyCalculator.calculate(cacheRequest, headers)
  }

  def calculateCurrentAge(request: CacheRequest, response: StoredResponse, requestTime: DateTime, responseTime: DateTime): Seconds = {
    currentAgeCalculator.calculateCurrentAge(request, response, requestTime, responseTime)
  }

  def calculateTimeToLive(request: Request, status: CacheableHttpResponseStatus, headers: CacheableHttpResponseHeaders): Option[DateTime] = {
    Some(DateTime.now.plusHours(24))
  }

  /**
   * Strips headers from the response before it is cached.
   */
  def calculateStrippedHeaders(originResponse: OriginResponse): Set[HeaderName] = {
    stripHeaderCalculator.stripHeaders(originResponse)
  }

  def generateCacheRequest(request: Request): CacheRequest = {
    val uri = request.getUri.toJavaNetURI
    val headers = ningHeadersToMap(request.getHeaders).map {
      case (name, values) =>
        (HeaderName(name), values)
    }
    val method = request.getMethod
    CacheRequest(uri = uri, method = method, headers = headers)
  }

  def generateStoredResponse(response: CacheableResponse, requestMethod: String, nominatedHeaders: Map[HeaderName, Seq[String]]) = {
    val uri: URI = response.getUri.toJavaNetURI
    val status: Int = response.getStatusCode
    val responseHeaders = response.getHeaders
    val headers = ningHeadersToMap(responseHeaders).map {
      case (name, values) =>
        (HeaderName(name), values)
    }

    StoredResponse(uri = uri,
      status = status,
      headers = headers,
      requestMethod = requestMethod,
      nominatedHeaders = nominatedHeaders)
  }

  def generateOriginResponse(request: Request, status: Int, responseHeaders: HttpResponseHeaders): OriginResponse = {
    val uri = request.getUri.toJavaNetURI
    val headers = ningHeadersToMap(responseHeaders.getHeaders).map {
      case (name, values) =>
        (HeaderName(name), values)
    }
    OriginResponse(uri, status, headers)
  }

  override def toString: String = {
    s"NingWSCache(${underlying.stats()})"
  }
}

object NingWSCache {

  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.NingWSCache")

  /**
   * Create a new Guava cache
   */
  def apply(config: AsyncHttpClientConfig): NingWSCache = {
    val cacheBuilder = GCacheBuilder.newBuilder()
    val gcache: GCache[CacheKey, CacheEntry] = cacheBuilder.build()
    apply(config, gcache)
  }

  /**
   * Create a new cache utilizing the given underlying Guava cache.
   * @param underlying a Guava cache
   */
  def apply(config: AsyncHttpClientConfig, underlying: GCache[CacheKey, CacheEntry]): NingWSCache = new NingWSCache(config, underlying)

}
