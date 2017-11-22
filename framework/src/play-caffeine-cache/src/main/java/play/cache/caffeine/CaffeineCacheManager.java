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

        // we create the cache builder
        // depending on weather the config for
        // the cache name was there or not
        if (config != null && config.hasPath(cacheName)) {
            cacheBuilder = CaffeineParser.from(config.getConfig(cacheName)).expireAfter(new CaffeineDefaultExpiry());
        } else {
            cacheBuilder = Caffeine.newBuilder().expireAfter(new CaffeineDefaultExpiry());
        }

        return cacheBuilder;
    }
}