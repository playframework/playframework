package play.api.libs.ws.ning.cache

import java.net.URI

import com.ning.http.client.Request

object CacheInvalidator {
  private val logger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ning.cache.CacheInvalidator")
}

/**
 * Invalidates entries when a response is returned in response to an
 * unsafe method (i.e. a POST request will invalidate entries)
 *
 * @see https://tools.ietf.org/html/rfc7234#section-4.4
 */
class CacheInvalidator(cache: NingWSCache) extends NingDebug {

  import CacheInvalidator._

  def invalidateIfUnsafe(request: Request, response: CacheableResponse): Unit = {
    logger.trace(s"invalidate: request = ${debug(request)}, response = ${debug(response)}")

    if (cache.isUnsafeMethod(request) && isNonErrorResponse(response)) {
      val requestHost = request.getUri.getHost

      //A cache MUST invalidate the effective request URI (Section 5.5 of
      //[RFC7230]) when it receives a non-error response to a request with a
      //method whose safety is unknown.
      val responseKey = CacheKey(request.getMethod, response.getUri.toJavaNetURI)
      invalidateKey(responseKey)

      //A cache MUST invalidate the effective Request URI (Section 5.5 of
      //[RFC7230]) as well as the URI(s) in the Location and Content-Location
      //response header fields (if present) when a non-error status code is
      //received in response to an unsafe request method.

      // https://tools.ietf.org/html/rfc7231#section-3.1.4.2
      // https://tools.ietf.org/html/rfc7230#section-5.5
      getURI(response, "Content-Location").foreach { contentLocation =>
        //However, a cache MUST NOT invalidate a URI from a Location or
        //Content-Location response header field if the host part of that URI
        //differs from the host part in the effective request URI (Section 5.5
        //of [RFC7230]).  This helps prevent denial-of-service attacks.
        if (requestHost.equalsIgnoreCase(contentLocation.getHost)) {
          val key = CacheKey(request.getMethod, contentLocation)
          invalidateKey(key)
        }
      }

      getURI(response, "Location").foreach { location =>
        if (requestHost.equalsIgnoreCase(location.getHost)) {
          val key = CacheKey(request.getMethod, location)
          invalidateKey(key)
        }
      }
    }
  }

  protected def invalidateKey(key: CacheKey): Unit = {
    logger.debug(s"invalidate: key = $key")
    cache.invalidateKey(key)
  }

  protected def getURI(response: CacheableResponse, headerName: String): Option[URI] = {
    Option(response.getHeaders.getFirstValue(headerName)).map { value =>
      // Gets the base URI, i.e. http://example.com/ so we can resolve relative URIs
      val baseURI = response.getUri.toJavaNetURI
      // So both absolute & relative URI will be resolved with example.com as base...
      baseURI.resolve(value)
    }
  }

  protected def isNonErrorResponse(response: CacheableResponse) = {
    //Here, a "non-error response" is one with a 2xx (Successful) or 3xx
    //(Redirection) status code.
    response.getStatusCode match {
      case success if success >= 200 && success < 300 =>
        true
      case redirect if redirect >= 300 && redirect < 400 =>
        true
      case other =>
        false
    }
  }

}
