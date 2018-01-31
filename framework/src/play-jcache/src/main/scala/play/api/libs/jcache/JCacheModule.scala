/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.jcache

import javax.cache.{ CacheManager, Caching }
import javax.inject._

import play.api.Environment
import play.api.inject._

/**
 * Provides bindings for JSR 107 (JCache) CacheManager.
 */
class JCacheModule extends SimpleModule(
  bind[CacheManager].toProvider[DefaultCacheManagerProvider]
)

/**
 * Provides the CacheManager as the output from Caching.getCachingProvider(env.classLoader).getCacheManager
 *
 * @param env the environment
 */
@Singleton
class DefaultCacheManagerProvider @Inject() (env: Environment) extends Provider[CacheManager] {
  lazy val get: CacheManager = {
    val provider = Caching.getCachingProvider(env.classLoader)
    provider.getCacheManager
  }
}
