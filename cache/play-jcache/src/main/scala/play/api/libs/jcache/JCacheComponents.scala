/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.jcache

import javax.cache.CacheManager
import javax.cache.Caching

import play.api.Environment

/**
 * Components for JCache CacheManager
 */
trait JCacheComponents {
  def environment: Environment

  lazy val cacheManager: CacheManager = Caching.getCachingProvider(environment.classLoader).getCacheManager
}
