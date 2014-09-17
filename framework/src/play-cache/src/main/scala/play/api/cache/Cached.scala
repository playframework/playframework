/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.cache

import org.apache.commons.codec.digest.DigestUtils

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{ Iteratee, Done }
import play.api.http.HeaderNames.{ IF_NONE_MATCH, ETAG, EXPIRES }
import play.api.mvc.Results.NotModified

import play.core.Execution.Implicits.internalContext

import scala.concurrent.duration._

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
 * @param caching A callback to get the number of seconds to cache results for
 */
case class Cached(key: RequestHeader => String, caching: PartialFunction[ResponseHeader, Duration]) {
  import Cached._

  def apply(action: EssentialAction)(implicit app: Application) = build(action)

  /**
   * Compose the cache with an action
   */
  def build(action: EssentialAction)(implicit app: Application) = EssentialAction { request =>
    val resultKey = key(request)
    val etagKey = s"$resultKey-etag"

    // Has the client a version of the resource as fresh as the last one we served?
    val notModified = for {
      requestEtag <- request.headers.get(IF_NONE_MATCH)
      etag <- Cache.getAs[String](etagKey)
      if requestEtag == "*" || etag == requestEtag
    } yield Done[Array[Byte], Result](NotModified)

    notModified.orElse {
      // Otherwise try to serve the resource from the cache, if it has not yet expired
      Cache.getAs[Result](resultKey).map(Done[Array[Byte], Result](_))
    }.getOrElse {
      // The resource was not in the cache, we have to run the underlying action
      val iterateeResult = action(request)

      // Add cache information to the response, so clients can cache its content
      iterateeResult.map(handleResult(_, etagKey, resultKey, app))
    }
  }

  /**
   * Eternity is one year long. Duration zero means eternity.
   */
  private val cachingWithEternity = caching.andThen { duration =>
    if (duration.zero) {
      Duration(60 * 60 * 24 * 365, SECONDS)
    } else {
      duration
    }
  }

  private def handleResult(result: Result, etagKey: String, resultKey: String, app: Application): Result = {
    cachingWithEternity.andThen { duration =>
      // Format expiration date according to http standard
      val expirationDate = http.dateFormat.print(System.currentTimeMillis() + duration.toMillis)
      // Generate a fresh ETAG for it
      // Use quoted sha1 hash of expiration date as ETAG
      val etag = s""""${DigestUtils.sha1Hex(expirationDate)}""""

      val resultWithHeaders = result.withHeaders(ETAG -> etag, EXPIRES -> expirationDate)

      // Cache the new ETAG of the resource
      Cache.set(etagKey, etag, duration)(app)
      // Cache the new Result of the resource
      Cache.set(resultKey, resultWithHeaders, duration)(app)

      resultWithHeaders
    }.applyOrElse(result.header, (_: ResponseHeader) => result)
  }

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result forever
   */
  def includeStatus(status: Int): Cached = includeStatus(status, Duration.Zero)

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration the number of seconds to cache the result for
   */
  def includeStatus(status: Int, duration: Int): Cached = includeStatus(status, Duration(duration, SECONDS))

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration how long should we cache the result for
   */
  def includeStatus(status: Int, duration: Duration): Cached = this.copy(caching = caching.orElse {
    case e if e.status == status => {
      duration
    }
  })

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration how long we should store responses
   */
  def default(duration: Duration): Cached = compose(PartialFunction((_: ResponseHeader) => duration))

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration the number of seconds we should store responses
   */
  def default(duration: Int): Cached = default(Duration(duration, SECONDS))

  /**
   * Compose the cache with new caching function
   * @param alternative a closure getting the reponseheader and returning the duration
   *        we should cache for
   */
  def compose(alternative: PartialFunction[ResponseHeader, Duration]): Cached = this.copy(caching = caching.orElse(alternative))

}

object Cached {

  /**
   * Convenient implicit class for adding a zero method to Duration
   */
  private implicit class DurationEmpty(d: Duration) {
    /**
     * This tests if the duration is currently zero
     * @returns boolean checking whether the duration is zero or not
     */
    def zero(): Boolean = d.neg().equals(d)
  }

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   */
  def apply(key: RequestHeader => String): Cached = {
    apply(key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   */
  def apply(key: String): Cached = {
    apply(_ => key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   */
  def apply(key: RequestHeader => String, duration: Int): Cached = {
    new Cached(key, { case (_: ResponseHeader) => Duration(duration, SECONDS) })
  }

  /**
   * A cached instance caching nothing
   * Useful for composition
   */
  def empty(key: RequestHeader => String): Cached = new Cached(key, PartialFunction.empty)

  /**
   * Caches everything, forever
   */
  def everything(key: RequestHeader => String): Cached = empty(key).default(0)

  /**
   * Caches everything for the specified seconds
   */
  def everything(key: RequestHeader => String, duration: Int): Cached = empty(key).default(duration)

  /**
   * Caches the specified status, for the specified number of seconds
   */
  def status(key: RequestHeader => String, status: Int, duration: Int): Cached = empty(key).includeStatus(status, Duration(duration, SECONDS))

  /**
   * Caches the specified status forever
   */
  def status(key: RequestHeader => String, status: Int): Cached = empty(key).includeStatus(status)
}

