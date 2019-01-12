/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.time.Instant
import javax.inject.Inject

import akka.stream.Materializer
import play.api._
import play.api.http.HeaderNames.{ ETAG, EXPIRES, IF_NONE_MATCH }
import play.api.libs.Codecs
import play.api.libs.streams.Accumulator
import play.api.mvc.Results.NotModified
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * A helper to add caching to an Action.
 */
class Cached @Inject() (cache: AsyncCacheApi)(implicit materializer: Materializer) {

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param caching Compute a cache duration from the resource header
   */
  def apply(
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration]): CachedBuilder = {
    new CachedBuilder(cache, key, caching)
  }

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
    apply((_: RequestHeader) => key, duration = 0)
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
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration
   */
  def apply(key: RequestHeader => String, duration: Duration): CachedBuilder = {
    new CachedBuilder(cache, key, { case (_: ResponseHeader) => duration })
  }

  /**
   * A cached instance caching nothing
   * Useful for composition
   */
  def empty(key: RequestHeader => String): CachedBuilder =
    new CachedBuilder(cache, key, PartialFunction.empty)

  /**
   * Caches everything, forever
   */
  def everything(key: RequestHeader => String): CachedBuilder =
    empty(key).default(0)

  /**
   * Caches everything for the specified seconds
   */
  def everything(key: RequestHeader => String, duration: Int): CachedBuilder =
    empty(key).default(duration)

  /**
   * Caches everything for the specified duration
   */
  def everything(key: RequestHeader => String, duration: Duration): CachedBuilder =
    empty(key).default(duration)

  /**
   * Caches the specified status, for the specified number of seconds
   */
  def status(key: RequestHeader => String, status: Int, duration: Int): CachedBuilder =
    empty(key).includeStatus(status, Duration(duration, SECONDS))

  /**
   * Caches the specified status, for the specified duration
   */
  def status(key: RequestHeader => String, status: Int, duration: Duration): CachedBuilder =
    empty(key).includeStatus(status, duration)

  /**
   * Caches the specified status forever
   */
  def status(key: RequestHeader => String, status: Int): CachedBuilder =
    empty(key).includeStatus(status)
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
    cache: AsyncCacheApi,
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration])(implicit materializer: Materializer) {

  /**
   * Compose the cache with an action
   */
  def apply(action: EssentialAction): EssentialAction = build(action)

  /**
   * Compose the cache with an action
   */
  def build(action: EssentialAction): EssentialAction = EssentialAction { request =>
    import play.core.Execution.Implicits.trampoline

    val resultKey = key(request)
    val etagKey = s"$resultKey-etag"

    def parseEtag(etag: String) = {
      val Etag = """(?:W/)?("[^"]*")""".r
      Etag.findAllMatchIn(etag).map(m => m.group(1)).toList
    }

    // Check if the client has a version as new as ours
    Accumulator.flatten(Future.successful(request.headers.get(IF_NONE_MATCH)).flatMap {
      case Some(requestEtag) =>
        cache.get[String](etagKey).map {
          case Some(etag) if requestEtag == "*" || parseEtag(requestEtag).contains(etag) => Some(Accumulator.done(NotModified))
          case _ => None
        }
      case None => Future.successful(None)
    }.flatMap {
      case Some(result) =>
        // The client has the most recent version
        Future.successful(result)
      case None =>
        // Otherwise try to serve the resource from the cache, if it has not yet expired
        cache.get[SerializableResult](resultKey).map { result =>
          result collect {
            case sr: SerializableResult => Accumulator.done(sr.result)
          }
        }.map {
          case Some(cachedResource) => cachedResource
          case None =>
            // The resource was not in the cache, so we have to run the underlying action
            val accumulatorResult = action(request)

            // Add cache information to the response, so clients can cache its content
            accumulatorResult.mapFuture(handleResult(_, etagKey, resultKey))
        }
    })
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

  private def handleResult(result: Result, etagKey: String, resultKey: String): Future[Result] = {
    import play.core.Execution.Implicits.trampoline

    cachingWithEternity.andThen { duration =>
      // Format expiration date according to http standard
      val expirationDate = http.dateFormat.format(Instant.ofEpochMilli(System.currentTimeMillis() + duration.toMillis))
      // Generate a fresh ETAG for it
      // Use quoted sha1 hash of expiration date as ETAG
      val etag = s""""${Codecs.sha1(expirationDate)}""""

      val resultWithHeaders = result.withHeaders(ETAG -> etag, EXPIRES -> expirationDate)

      for {
        // Cache the new ETAG of the resource
        _ <- cache.set(etagKey, etag, duration)
        // Cache the new Result of the resource
        _ <- cache.set(resultKey, new SerializableResult(resultWithHeaders), duration)
      } yield resultWithHeaders

    }.applyOrElse(result.header, (_: ResponseHeader) => Future.successful(result))
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
  def default(duration: Duration): CachedBuilder = compose({ case _: ResponseHeader => duration })

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
