/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.cache

import javax.inject.Inject
import play.api._
import play.api.mvc._
import play.api.libs.Codecs
import play.api.libs.iteratee.{ Iteratee, Done }
import play.api.http.HeaderNames.{ IF_NONE_MATCH, ETAG, EXPIRES }
import play.api.mvc.Results.NotModified

import play.core.Execution.Implicits.internalContext

import scala.concurrent.duration._

/**
 * A helper to add caching to an Action.
 */
class Cached @Inject() (cache: CacheApi) {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param caching Compute a cache duration from the respone header
   */
  def apply(
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration]): CachedBuilder = new CachedBuilder(cache, key, caching)

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   */
  def apply(key: RequestHeader => String): CachedBuilder = {
    apply(key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   */
  def apply(key: String): CachedBuilder = {
    apply(_ => key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   */
  def apply(key: RequestHeader => String, duration: Int): CachedBuilder = {
    new CachedBuilder(cache, key, { case (_: ResponseHeader) => Duration(duration, SECONDS) })
  }

  /**
   * A cached instance caching nothing
   * Useful for composition
   */
  def empty(key: RequestHeader => String): CachedBuilder = new CachedBuilder(cache, key, PartialFunction.empty)

  /**
   * Caches everything, forever
   */
  def everything(key: RequestHeader => String): CachedBuilder = empty(key).default(0)

  /**
   * Caches everything for the specified seconds
   */
  def everything(key: RequestHeader => String, duration: Int): CachedBuilder = empty(key).default(duration)

  /**
   * Caches the specified status, for the specified number of seconds
   */
  def status(key: RequestHeader => String, status: Int, duration: Int): CachedBuilder = empty(key).includeStatus(status, Duration(duration, SECONDS))

  /**
   * Caches the specified status forever
   */
  def status(key: RequestHeader => String, status: Int): CachedBuilder = empty(key).includeStatus(status)
}

/**
 * A helper to add caching to an Action. This helper uses the Application's default cache.
 * If you want to inject a custom cache, see the `Cached` class.
 */
object Cached {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param caching Compute a cache duration from the respone header
   */
  def apply(
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration]): UnboundCachedBuilder = new UnboundCachedBuilder(key, caching)

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   */
  def apply(key: RequestHeader => String): UnboundCachedBuilder = {
    apply(key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   */
  def apply(key: String): UnboundCachedBuilder = {
    apply(_ => key, duration = 0)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration (in seconds)
   */
  def apply(key: RequestHeader => String, duration: Int): UnboundCachedBuilder = {
    new UnboundCachedBuilder(key, { case (_: ResponseHeader) => Duration(duration, SECONDS) })
  }

  /**
   * A cached instance caching nothing
   * Useful for composition
   */
  def empty(key: RequestHeader => String): UnboundCachedBuilder = new UnboundCachedBuilder(key, PartialFunction.empty)

  /**
   * Caches everything, forever
   */
  def everything(key: RequestHeader => String): UnboundCachedBuilder = empty(key).default(0)

  /**
   * Caches everything for the specified seconds
   */
  def everything(key: RequestHeader => String, duration: Int): UnboundCachedBuilder = empty(key).default(duration)

  /**
   * Caches the specified status, for the specified number of seconds
   */
  def status(key: RequestHeader => String, status: Int, duration: Int): UnboundCachedBuilder = empty(key).includeStatus(status, Duration(duration, SECONDS))

  /**
   * Caches the specified status forever
   */
  def status(key: RequestHeader => String, status: Int): UnboundCachedBuilder = empty(key).includeStatus(status)
}

/**
 * Builds an action with caching behavior. Typically created with one of the methods in the `Cached`
 * class. Uses both server and client caches:
 *
 *  - Adds an `Expires` header to the response, so clients can cache response content ;
 *  - Adds an `Etag` header to the response, so clients can cache response content and ask the server for freshness ;
 *  - Cache the result on the server, so the underlying action is not computed at each call.
 *
 * @param cache The cache used for caching results
 * @param key Compute a key from the request header
 * @param caching A callback to get the number of seconds to cache results for
 */
final class CachedBuilder(
    cache: CacheApi,
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration]) {

  /**
   * Compose the cache with an action
   */
  def apply(action: EssentialAction): EssentialAction = build(action)

  /**
   * Compose the cache with an action
   */
  def build(action: EssentialAction): EssentialAction = EssentialAction { request =>
    val resultKey = key(request)
    val etagKey = s"$resultKey-etag"

    // Has the client a version of the resource as fresh as the last one we served?
    val notModified = for {
      requestEtag <- request.headers.get(IF_NONE_MATCH)
      etag <- cache.get[String](etagKey)
      if requestEtag == "*" || etag == requestEtag
    } yield Done[Array[Byte], Result](NotModified)

    notModified.orElse {
      // Otherwise try to serve the resource from the cache, if it has not yet expired
      cache.get[SerializableResult](resultKey).map { sr: SerializableResult =>
        Done[Array[Byte], Result](sr.result)
      }
    }.getOrElse {
      // The resource was not in the cache, we have to run the underlying action
      val iterateeResult = action(request)

      // Add cache information to the response, so clients can cache its content
      iterateeResult.map(handleResult(_, etagKey, resultKey))
    }
  }

  /**
   * Eternity is one year long. Duration zero means eternity.
   */
  private val cachingWithEternity = caching.andThen { duration =>
    // FIXME: Surely Duration.Inf is a better marker for eternity than 0?
    val zeroDuration: Boolean = duration.neg().equals(duration)
    if (zeroDuration) {
      Duration(60 * 60 * 24 * 365, SECONDS)
    } else {
      duration
    }
  }

  private def handleResult(result: Result, etagKey: String, resultKey: String): Result = {
    cachingWithEternity.andThen { duration =>
      // Format expiration date according to http standard
      val expirationDate = http.dateFormat.print(System.currentTimeMillis() + duration.toMillis)
      // Generate a fresh ETAG for it
      // Use quoted sha1 hash of expiration date as ETAG
      val etag = s""""${Codecs.sha1(expirationDate)}""""

      val resultWithHeaders = result.withHeaders(ETAG -> etag, EXPIRES -> expirationDate)

      // Cache the new ETAG of the resource
      cache.set(etagKey, etag, duration)
      // Cache the new Result of the resource
      cache.set(resultKey, new SerializableResult(resultWithHeaders), duration)

      resultWithHeaders
    }.applyOrElse(result.header, (_: ResponseHeader) => result)
  }

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result forever
   */
  def includeStatus(status: Int): CachedBuilder = includeStatus(status, Duration.Zero)

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration the number of seconds to cache the result for
   */
  def includeStatus(status: Int, duration: Int): CachedBuilder = includeStatus(status, Duration(duration, SECONDS))

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration how long should we cache the result for
   */
  def includeStatus(status: Int, duration: Duration): CachedBuilder = compose {
    case e if e.status == status => {
      duration
    }
  }

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration how long we should store responses
   */
  def default(duration: Duration): CachedBuilder = compose(PartialFunction((_: ResponseHeader) => duration))

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration the number of seconds we should store responses
   */
  def default(duration: Int): CachedBuilder = default(Duration(duration, SECONDS))

  /**
   * Compose the cache with new caching function
   * @param alternative a closure getting the reponseheader and returning the duration
   *        we should cache for
   */
  def compose(alternative: PartialFunction[ResponseHeader, Duration]): CachedBuilder = new CachedBuilder(
    cache = cache,
    key = key,
    caching = caching.orElse(alternative)
  )

}

/**
 * Builds an action with caching behavior. Typically created with one of the methods in the `Cached`
 * companion object. Uses both server and client caches:
 *
 *  - Adds an `Expires` header to the response, so clients can cache response content ;
 *  - Adds an `Etag` header to the response, so clients can cache response content and ask the server for freshness ;
 *  - Cache the result on the server, so the underlying action is not computed at each call.
 *
 * Unlike `CachedBuilder`, an `UnboundCachedBuilder` isn't bound to a particular
 * cache when it is created. It binds the default cache of the current application
 * when it builds an action.
 *
 * @param key Compute a key from the request header
 * @param caching A callback to get the number of seconds to cache results for
 */
class UnboundCachedBuilder(key: RequestHeader => String, caching: PartialFunction[ResponseHeader, Duration]) {
  import Cached._

  /**
   * Compose the cache with an action
   */
  def apply(action: EssentialAction)(implicit app: Application): EssentialAction = build(action)

  /**
   * Compose the cache with an action
   */
  def build(action: EssentialAction)(implicit app: Application): EssentialAction = {
    new CachedBuilder(Cache.cacheApi, key, caching).build(action)
  }

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result forever
   */
  def includeStatus(status: Int): UnboundCachedBuilder = includeStatus(status, Duration.Zero)

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration the number of seconds to cache the result for
   */
  def includeStatus(status: Int, duration: Int): UnboundCachedBuilder = includeStatus(status, Duration(duration, SECONDS))

  /**
   * Whether this cache should cache the specified response if the status code match
   * This method will cache the result for duration seconds
   *
   * @param status the status code to check
   * @param duration how long should we cache the result for
   */
  def includeStatus(status: Int, duration: Duration): UnboundCachedBuilder = compose {
    case e if e.status == status => {
      duration
    }
  }

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration how long we should store responses
   */
  def default(duration: Duration): UnboundCachedBuilder = compose(PartialFunction((_: ResponseHeader) => duration))

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration the number of seconds we should store responses
   */
  def default(duration: Int): UnboundCachedBuilder = default(Duration(duration, SECONDS))

  /**
   * Compose the cache with new caching function
   * @param alternative a closure getting the reponseheader and returning the duration
   *        we should cache for
   */
  def compose(alternative: PartialFunction[ResponseHeader, Duration]): UnboundCachedBuilder = new UnboundCachedBuilder(
    key = key,
    caching = caching.orElse(alternative)
  )

}
