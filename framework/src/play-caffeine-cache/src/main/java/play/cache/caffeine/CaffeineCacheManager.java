/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.cache.caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

public class CaffeineCacheManager {
    private final ConcurrentMap<String, NamedCaffeineCache> cacheMap = new ConcurrentHashMap<>(16);
    private Config config;

    public CaffeineCacheManager(Config config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public <K, V> NamedCaffeineCache<K, V> getCache(String cacheName) {
        NamedCaffeineCache<K, V> namedCache = cacheMap.getOrDefault(cacheName, null);

        // if the cache is null we have to create it
        if (namedCache == null) {
            Caffeine cacheBuilder = getCacheBuilder(cacheName);

            namedCache = new NamedCaffeineCache(cacheName, cacheBuilder.build());

            cacheMap.put(cacheName, namedCache);
        }

        return namedCache;
    }

    private Caffeine getCacheBuilder(String cacheName) {
        Caffeine cacheBuilder;
        CaffeineDefaultExpiry defaultExpiry = new CaffeineDefaultExpiry();
        Config caches = config.getConfig("caches");
        Config defaults = config.getConfig("defaults");
        Config cacheConfig;

        if (caches.hasPath(cacheName)) {
            cacheConfig = caches.getConfig(cacheName).withFallback(defaults);
        } else {
            cacheConfig = defaults;
        }

        cacheBuilder = CaffeineParser.from(cacheConfig).expireAfter(defaultExpiry);

        return cacheBuilder;
    }
}