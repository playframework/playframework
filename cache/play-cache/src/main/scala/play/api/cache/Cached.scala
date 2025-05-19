/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.Future

import jakarta.inject.Inject
import org.apache.pekko.stream.Materializer
import play.api._
import play.api.http.HeaderNames.ETAG
import play.api.http.HeaderNames.EXPIRES
import play.api.http.HeaderNames.IF_NONE_MATCH
import play.api.libs.streams.Accumulator
import play.api.libs.Codecs
import play.api.mvc._
import play.api.mvc.Results.NotModified

/**
 * A helper to add caching to an Action.
 * @param hashResponse If true `Etag` is calculated based on response content, otherwise it uses expiration date
 */
class Cached(cache: AsyncCacheApi, hashResponse: Boolean)(implicit materializer: Materializer) {

  @Inject() def this(cache: AsyncCacheApi)(implicit materializer: Materializer) = this(cache, false)

  /**
   * Cache an action.
   *
   * @param key Compute a key from the request header
   * @param caching Compute a cache duration from the resource header
   */
  def apply(key: RequestHeader => String, caching: PartialFunction[ResponseHeader, Duration]): CachedBuilder = {
    new CachedBuilder(cache, key, caching, hashResponse)
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
    new CachedBuilder(cache, key, { case (_: ResponseHeader) => Duration(duration, SECONDS) }, hashResponse)
  }

  /**
   * Cache an action.
   *
   * @param key Cache key
   * @param duration Cache duration
   */
  def apply(key: RequestHeader => String, duration: Duration): CachedBuilder = {
    new CachedBuilder(cache, key, { case (_: ResponseHeader) => duration }, hashResponse)
  }

  /**
   * A cached instance caching nothing
   * Useful for composition
   */
  def empty(key: RequestHeader => String): CachedBuilder =
    new CachedBuilder(cache, key, PartialFunction.empty, hashResponse)

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
 * @param hashResponse If true `Etag` is calculated based on response content, otherwise it uses expiration date
 */
final class CachedBuilder(
    cache: AsyncCacheApi,
    key: RequestHeader => String,
    caching: PartialFunction[ResponseHeader, Duration],
    hashResponse: Boolean
)(implicit materializer: Materializer) {

  def this(cache: AsyncCacheApi, key: RequestHeader => String, caching: PartialFunction[ResponseHeader, Duration])(
      implicit materializer: Materializer
  ) = this(cache, key, caching, false)

  def withHashedResponse: CachedBuilder = new CachedBuilder(cache, key, caching, true)

  /**
   * Compose the cache with an action
   */
  def apply(action: EssentialAction): EssentialAction = build(action)

  private val Etag = """(?:W/)?("[^"]*")""".r

  /**
   * Compose the cache with an action
   */
  def build(action: EssentialAction): EssentialAction = EssentialAction { request =>
    import play.core.Execution.Implicits.trampoline

    val resultKey  = key(request)
    val headersKey = s"$resultKey-headers"

    def parseEtag(etag: String) =
      if (etag == "*") List(etag)
      else Etag.findAllMatchIn(etag).map(_.group(1)).toList

    val etags = request.headers.get(IF_NONE_MATCH).toList.flatMap(parseEtag)
    def requestMatches(resultHeaders: Map[String, String]) = {
      val etag = resultHeaders.get(ETAG)
      etags.exists(t => t == "*" || etag.contains(t))
    }

    def notModifiedResult(headers: Map[String, String]) = NotModified.withHeaders(headers.toSeq*)
    def matchResult(result: Result) =
      if (requestMatches(result.header.headers))
        notModifiedResult(result.header.headers) // The client already has the same result
      else result

    Accumulator.flatten {
      for {
        // Check if the client has a version as new as ours
        resultHeaders <- cache.get[Map[String, String]](headersKey)
        result <- resultHeaders match {
          case Some(resultHeaders) if requestMatches(resultHeaders) =>
            // The client has the most recent version
            Future.successful(Accumulator.done(notModifiedResult(resultHeaders)))
          case _ =>
            // Otherwise try to serve the resource from the cache, if it has not yet expired
            cache.get[SerializableResult](resultKey).map {
              case Some(sr) =>
                Accumulator.done(matchResult(sr.result))
              case None =>
                // The resource was not in the cache, so we have to run the underlying action
                action(request).mapFuture(handleResult(_, headersKey, resultKey)).map(matchResult)
            }
        }
      } yield result
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

  private def handleResult(result: Result, headersKey: String, resultKey: String): Future[Result] = {
    import play.core.Execution.Implicits.trampoline

    cachingWithEternity
      .andThen { duration =>
        val expirationDate =
          http.dateFormat.format(Instant.ofEpochMilli(System.currentTimeMillis() + duration.toMillis))
        val etagString = if (hashResponse) {
          val stream = new java.security.DigestOutputStream(_ => (), java.security.MessageDigest.getInstance("SHA-1"))
          val oos    = new java.io.ObjectOutputStream(stream)
          new SerializableResult(result).writeExternal(oos)
          oos.close()
          stream.close()
          Codecs.toHexString(stream.getMessageDigest.digest())
        } else {
          Codecs.sha1(expirationDate)
        }
        val etag              = s""""$etagString""""
        val resultWithHeaders = result.withHeaders(ETAG -> etag, EXPIRES -> expirationDate)
        for {
          // Cache the headers of the resource
          _ <- cache.set(headersKey, resultWithHeaders.header.headers, duration)
          // Cache the new Result of the resource
          _ <- cache.set(resultKey, new SerializableResult(resultWithHeaders), duration)
        } yield resultWithHeaders
      }
      .applyOrElse(result.header, (_: ResponseHeader) => Future.successful(result))
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
  def default(duration: Duration): CachedBuilder = compose { case _: ResponseHeader => duration }

  /**
   * The returned cache will store all responses whatever they may contain
   * @param duration the number of seconds we should store responses
   */
  def default(duration: Int): CachedBuilder = default(Duration(duration, SECONDS))

  /**
   * Compose the cache with new caching function
   * @param alternative a closure getting the response header and returning the duration
   *        we should cache for
   */
  def compose(alternative: PartialFunction[ResponseHeader, Duration]): CachedBuilder = new CachedBuilder(
    cache = cache,
    key = key,
    caching = caching.orElse(alternative),
    hashResponse = hashResponse
  )
}
