package play.api.cache

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{ Iteratee, Done }
import play.api.http.HeaderNames.{ IF_NONE_MATCH, ETAG, EXPIRES }
import play.api.mvc.Results.NotModified

import play.core.Execution.Implicits.internalContext

/**
 * Cache an action.
 *
 * Uses both server and client caches:
 *
 *  - Adds an `Expires` header to the response, so clients can cache response content ;
 *  - Adds an `Etag` header to the response, so clients can cache response content and ask the server for freshness ;
 *  - Cache the result on the server, so the underlying action is not computed at each call.
 *
 * @param key Compute a key from the request header
 * @param duration Cache duration (in seconds, 0 means eternity)
 * @param action Action to cache
 */
case class Cached(key: RequestHeader => String, duration: Int)(action: EssentialAction)(implicit app: Application) extends EssentialAction {

  def apply(request: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {

    val resultKey = key(request)
    val etagKey = s"$resultKey-etag"

    // Has the client a version of the resource as fresh as the last one we served?
    val notModified = for {
      requestEtag <- request.headers.get(IF_NONE_MATCH)
      etag <- Cache.getAs[String](etagKey)
      if requestEtag == "*" || etag == requestEtag
    } yield Done[Array[Byte], SimpleResult](NotModified)

    notModified.orElse(
      // Otherwise try to serve the resource from the cache, if it has not yet expired
      Cache.getAs[SimpleResult](resultKey).map(Done[Array[Byte], SimpleResult](_))
    ).getOrElse {
        // The resource was not in the cache, we have to run the underlying action
        val iterateeResult = action(request)
        val durationMilliseconds = if (duration == 0) 1000 * 60 * 60 * 24 * 365 else duration * 1000 // Set client cache expiration to one year for “eternity” duration
        val expirationDate = http.dateFormat.print(System.currentTimeMillis() + durationMilliseconds)
        // Generate a fresh ETAG for it
        val etag = expirationDate // Use the expiration date as ETAG
        // Add cache information to the response, so clients can cache its content
        iterateeResult.map { result =>
          val resultWithHeaders = result.withHeaders(ETAG -> etag, EXPIRES -> expirationDate)
          Cache.set(etagKey, etag, duration) // Cache the new ETAG of the resource
          Cache.set(resultKey, resultWithHeaders, duration) // Cache the new SimpleResult of the resource
          resultWithHeaders
        }
      }
  }

}

object Cached {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param action Action to cache
   */
  def apply(key: RequestHeader => String)(action: EssentialAction)(implicit app: Application): Cached = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param action Action to cache
   */
  def apply(key: String)(action: EssentialAction)(implicit app: Application): Cached = {
    apply(key, duration = 0)(action)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   * @param action Action to cache
   */
  def apply(key: String, duration: Int)(action: EssentialAction)(implicit app: Application): Cached = {
    Cached(_ => key, duration)(action)
  }

}