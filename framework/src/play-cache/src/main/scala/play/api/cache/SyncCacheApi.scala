/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import javax.inject.Inject

import scala.concurrent.duration.{ Duration, _ }
import scala.concurrent.{ Await, Future }
import scala.reflect.ClassTag

/**
 * A cache API that uses synchronous calls rather than async calls. Useful when you know you have a fast in-memory cache.
 */
trait SyncCacheApi {

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time.
   */
  def set(key: String, value: Any, expiration: Duration = Duration.Inf): Unit

  /**
   * Remove a value from the cache
   */
  def remove(key: String): Unit

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => A): A

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  def get[T: ClassTag](key: String): Option[T]
}

/**
 * A SyncCacheApi that wraps an AsyncCacheApi
 */
class DefaultSyncCacheApi @Inject() (val cacheApi: AsyncCacheApi) extends SyncCacheApi {

  protected val awaitTimeout: Duration = 5.seconds

  def set(key: String, value: Any, expiration: Duration): Unit = {
    Await.result(cacheApi.set(key, value, expiration), awaitTimeout)
  }

  def get[T: ClassTag](key: String): Option[T] = {
    Await.result(cacheApi.get(key), awaitTimeout)
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => A): A = {
    Await.result(cacheApi.getOrElseUpdate(key, expiration)(Future.successful(orElse)), awaitTimeout)
  }

  def remove(key: String): Unit = {
    Await.result(cacheApi.remove(key), awaitTimeout)
  }
}
