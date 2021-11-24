/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.config.Config
import play.cache.caffeine.CaffeineParser
import play.cache.caffeine.NamedCaffeineCache

import java.util.Collections

class CaffeineCacheManager(private var config: Config) {
  private val cacheMap: ConcurrentMap[String, NamedCaffeineCache[_, _]] =
    new ConcurrentHashMap(16)

  def getCache[K, V](cacheName: String): NamedCaffeineCache[K, V] = {
    var namedCache: NamedCaffeineCache[K, V] =
      cacheMap.getOrDefault(cacheName, null).asInstanceOf[NamedCaffeineCache[K, V]]
    // if the cache is null we have to create it

    if (namedCache == null) {
      val cacheBuilder: Caffeine[K, V] = getCacheBuilder(cacheName).asInstanceOf[Caffeine[K, V]]
      namedCache = new NamedCaffeineCache[K, V](cacheName, cacheBuilder.buildAsync().asInstanceOf[AsyncCache[K, V]])
      cacheMap.put(cacheName, namedCache.asInstanceOf[NamedCaffeineCache[_, _]])
    }
    namedCache
  }

  /* JAVA API */
  def getCacheNames(): java.util.Set[String] = {
    Collections.unmodifiableSet(cacheMap.keySet())
  }

  def cacheNames: Set[String] = {
    scala.collection.JavaConverters.asScalaSet(cacheMap.keySet()).toSet
  }

  private[caffeine] def getCacheBuilder(cacheName: String): Caffeine[_, _] = {
    var cacheBuilder: Caffeine[_, _]         = null
    val defaultExpiry: DefaultCaffeineExpiry = new DefaultCaffeineExpiry
    val caches: Config                       = config.getConfig("caches")
    val defaults: Config                     = config.getConfig("defaults")
    var cacheConfig: Config                  = null
    cacheConfig =
      if (caches.hasPath(cacheName))
        caches.getConfig(cacheName).withFallback(defaults)
      else defaults
    cacheBuilder = CaffeineParser.from(cacheConfig).expireAfter(defaultExpiry)
    cacheBuilder
  }
}
