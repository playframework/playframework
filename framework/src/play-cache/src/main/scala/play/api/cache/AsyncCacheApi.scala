/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import akka.Done

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * The cache API
 */
trait AsyncCacheApi {

  /**
   * Get an instance of [[SyncCacheApi]] to make synchronous calls.
   */
  lazy val sync: SyncCacheApi = new DefaultSyncCacheApi(this)

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time.
   */
  def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Done]

  /**
   * Remove a value from the cache
   */
  def remove(key: String): Future[Done]

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was not found in cache.
   */
  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => Future[A]): Future[A]

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as a future of Option[T]
   */
  def get[T: ClassTag](key: String): Future[Option[T]]

  /**
   * Removes all values from the cache. This may be useful as an admin user operation if it is supported by your cache.
   *
   * @throws UnsupportedOperationException if this cache implementation does not support removing all values.
   * @return a Future[Done], which is completed with either a Done or an exception if the clear did not work.
   */
  def removeAll(): Future[Done]
}
