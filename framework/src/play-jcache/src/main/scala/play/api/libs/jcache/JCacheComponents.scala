/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.jcache

import javax.cache.{ CacheManager, Caching }

import play.api.Environment

/**
 * Components for JCache CacheManager
 */
trait JCacheComponents {

  def environment: Environment

  lazy val cacheManager: CacheManager = Caching.getCachingProvider(environment.classLoader).getCacheManager
}
