/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache.caffeine

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.Collections

import akka.actor.ActorSystem
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.config.Config
import play.cache.caffeine.CaffeineParser
import play.cache.caffeine.NamedCaffeineCache

class CaffeineCacheManager(private val config: Config, private val actorSystem: ActorSystem) {
  private val cacheMap: ConcurrentMap[String, NamedCaffeineCache[_, _]] =
    new ConcurrentHashMap(16)

  def getCache[K, V](cacheName: String): NamedCaffeineCache[K, V] = {
    cacheMap
      .computeIfAbsent(
        cacheName,
        cacheName => {
          val cacheBuilder: Caffeine[K, V] = getCacheBuilder(cacheName).asInstanceOf[Caffeine[K, V]]
          val namedCache                   =
            new NamedCaffeineCache[K, V](cacheName, cacheBuilder.buildAsync().asInstanceOf[AsyncCache[K, V]])
          namedCache.asInstanceOf[NamedCaffeineCache[_, _]]
        }
      )
      .asInstanceOf[NamedCaffeineCache[K, V]]
  }

  /* JAVA API */
  def getCacheNames(): java.util.Set[String] = {
    Collections.unmodifiableSet(cacheMap.keySet())
  }

  def cacheNames: Set[String] = {
    scala.jdk.CollectionConverters.SetHasAsScala(cacheMap.keySet()).asScala.toSet
  }

  private[caffeine] def getCacheBuilder(cacheName: String): Caffeine[_, _] = {
    val defaultExpiry: DefaultCaffeineExpiry = new DefaultCaffeineExpiry
    val caches: Config                       = config.getConfig("caches")
    val defaults: Config                     = config.getConfig("defaults")
    val cacheConfig                          =
      if (caches.hasPath(cacheName))
        caches.getConfig(cacheName).withFallback(defaults)
      else defaults
    CaffeineParser.from(cacheConfig, actorSystem).expireAfter(defaultExpiry)
  }
}
