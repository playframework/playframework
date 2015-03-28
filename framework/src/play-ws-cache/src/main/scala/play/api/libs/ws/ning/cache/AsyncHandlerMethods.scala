package play.api.libs.ws.ning.cache

import com.ning.http.client.{ RequestBuilder, FluentCaseInsensitiveStringsMap, Request }
import com.typesafe.cachecontrol._
import org.joda.time.{ Seconds, DateTime }
import org.slf4j.Logger

/**
 * Useful AsyncHandler methods
 */
trait AsyncHandlerMethods extends NingDebug {

  def cache: NingWSCache

  def stripHeaders(request: Request, httpResponse: CacheableResponse)(implicit logger: Logger): CacheableResponse = {
    val originResponse = cache.generateOriginResponse(request, httpResponse.getStatusCode, httpResponse.headers)
    val stripSet = cache.calculateStrippedHeaders(originResponse)

    val r = if (stripSet.nonEmpty) {
      import scala.collection.JavaConverters._
      val stripHeaderNames = stripSet.map(_.toString()).asJavaCollection
      logger.debug(s"massageCachedResponse: stripHeaderNames = $stripHeaderNames")
      val strippedHeaders = httpResponse.getHeaders.deleteAll(stripHeaderNames)
      logger.debug(s"massageCachedResponse: strippedHeaders = $strippedHeaders")
      val isTrailing = httpResponse.headers.isTraillingHeadersReceived
      val newHeaders = new CacheableHttpResponseHeaders(isTrailing, strippedHeaders)
      httpResponse.copy(headers = newHeaders)
    } else {
      httpResponse
    }
    r
  }

  def calculateSecondaryKeys(request: Request, r: CacheableResponse)(implicit logger: Logger): Map[HeaderName, Seq[String]] = {
    val nominated = cache.calculateSecondaryKeys(request, r)
    nominated.getOrElse(Map())
  }

  def cacheResponse(request: Request, response: CacheableResponse)(implicit logger: Logger): Unit = {
    logger.debug(s"cacheResponse: caching response ${debug(response)}")

    val nominated = calculateSecondaryKeys(request, response)
    val ttl = timeToLive(request, response)
    val entry = new CacheEntry(response, request.getMethod, nominated, ttl)
    cache.put(CacheKey(request), entry)
  }

  /**
   * Calculates when the cache will remove the time to live of the cache.  Note that this is different
   * from the expiration date or the freshness lifetime.
   */
  def timeToLive(request: Request, response: CacheableResponse): Option[DateTime] = {
    cache.calculateTimeToLive(request, response.status, response.headers)
  }

  def isUncachedResponse(any: Any): Boolean = {
    any match {
      case chrs: CacheableHttpResponseStatus =>
        false
      case headers: CacheableHttpResponseHeaders =>
        false
      case bodyPart: CacheableHttpResponseBodyPart =>
        false
      case response: CacheableResponse =>
        false
      case _ =>
        true
    }
  }

  protected def freshenResponse(newHeaders: FluentCaseInsensitiveStringsMap, storedResponse: CacheableResponse)(implicit logger: Logger): CacheableResponse = {
    if (logger.isTraceEnabled) {
      logger.trace(s"freshenResponse: newHeaders = $newHeaders, storedResponse = $storedResponse")
    }

    import collection.JavaConverters._

    // Need to freshen this stale response
    // https://tools.ietf.org/html/rfc7234#section-4.3.4
    //If a stored response is selected for update, the cache MUST:
    //o  delete any Warning header fields in the stored response with
    //warn-code 1xx (see Section 5.5);
    //
    //o  retain any Warning header fields in the stored response with
    //warn-code 2xx; and,
    val headers = storedResponse.headers
    val headersMap = new FluentCaseInsensitiveStringsMap(headers.getHeaders)
    val filteredWarnings = headersMap.get("Warning").asScala.filter { line =>
      val warning = WarningParser.parse(line)
      warning.code < 200
    }.asJava
    headersMap.put("Warning", filteredWarnings)

    //o  use other header fields provided in the 304 (Not Modified)
    //response to replace all instances of the corresponding header
    //fields in the stored response.
    headersMap.replaceAll(newHeaders)

    val updatedHeaders = headers.copy(headers = headersMap)
    storedResponse.copy(headers = updatedHeaders)
  }

  protected def generateCachedResponse(request: Request, requestTime: DateTime, entry: CacheEntry): CacheableResponse = {
    val cacheRequest = cache.generateCacheRequest(request)
    val storedResponse = cache.generateStoredResponse(entry.response, entry.requestMethod, entry.nominatedHeaders)
    val currentAge = cache.calculateCurrentAge(cacheRequest, storedResponse, requestTime, responseTime = HttpDate.now)
    val freshnessCalculator = new FreshnessCalculator(cache)
    val freshnessLifetime = freshnessCalculator.calculateFreshnessLifetime(cacheRequest, storedResponse)
    val isFresh = freshnessLifetime.isGreaterThan(currentAge)

    val finalResponse = if (isFresh) {
      addFreshHeaders(entry.response, currentAge)
    } else {
      addStaleHeaders(entry.response, currentAge)
    }
    finalResponse
  }

  def addFreshHeaders(response: CacheableResponse, age: Seconds): CacheableResponse = {
    replaceHeaders(response) { headers =>
      //    When a stored response is used to satisfy a request without
      //    validation, a cache MUST generate an Age header field (Section 5.1),
      //    replacing any present in the response with a value equal to the
      //    stored response's current_age; see Section 4.2.3.
      headers.replaceWith("Age", age.getSeconds.toString)
    }
  }

  def addStaleHeaders(response: CacheableResponse, age: Seconds): CacheableResponse = {
    replaceHeaders(response) { headers =>
      //    When a stored response is used to satisfy a request without
      //    validation, a cache MUST generate an Age header field (Section 5.1),
      //    replacing any present in the response with a value equal to the
      //    stored response's current_age; see Section 4.2.3.
      headers.replaceWith("Age", age.getSeconds.toString)

      //    A cache SHOULD generate a Warning header field with the 110 warn-code
      //    (see Section 5.5.1) in stale responses.  Likewise, a cache SHOULD
      //    generate a 112 warn-code (see Section 5.5.3) in stale responses if
      //      the cache is disconnected.
      //    A cache SHOULD NOT generate a new Warning header field when
      //      forwarding a response that does not have an Age header field, even if
      //      the response is already stale.  A cache need not validate a response
      //    that merely became stale in transit.
      headers.add("Warning", Warning(110, "-", "Response is Stale", None).toString())
    }
  }

  def addRevalidationFailed(response: CacheableResponse): CacheableResponse = {
    replaceHeaders(response) { headers =>
      headers.add("Warning", Warning(111, "-", "Revalidation Failed", None).toString())
    }
  }

  def addDisconnectHeader(response: CacheableResponse): CacheableResponse = {
    replaceHeaders(response) { headers =>
      headers.add("Warning", Warning(112, "-", "Disconnected Operation", None).toString())
    }
  }

  def replaceHeaders(response: CacheableResponse)(block: FluentCaseInsensitiveStringsMap => FluentCaseInsensitiveStringsMap): CacheableResponse = {
    val newHeadersMap = block(new FluentCaseInsensitiveStringsMap(response.getHeaders))

    val cachedHeaders = response.headers
    val newHeaders = cachedHeaders.copy(headers = newHeadersMap)
    response.copy(headers = newHeaders)
  }
}
