/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.cache.caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

import static play.cache.caffeine.CaffeineConfigConstants.*;

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
        CaffeineDefaultExpiry defaultExpiry = new CaffeineDefaultExpiry();

        // we create the cache builder
        // depending on whether the config for
        // the cache name was there or not
        // if not we look for default config
        if (config != null) {
            String pathKey = "caches." + cacheName;
            System.out.println(pathKey);
            if (config.hasPath(pathKey)) {
                cacheBuilder = CaffeineParser.from(config.getConfig(pathKey)).expireAfter(defaultExpiry);
            } else if (config.hasPath(PLAY_CACHE_CAFFEINE_DEFAULTS)) {
                cacheBuilder = CaffeineParser.from(config.getConfig(PLAY_CACHE_CAFFEINE_DEFAULTS)).expireAfter(defaultExpiry);
            } else {
                cacheBuilder = Caffeine.newBuilder().expireAfter(defaultExpiry);
            }
        } else {
            cacheBuilder = Caffeine.newBuilder().expireAfter(defaultExpiry);
        }

        return cacheBuilder;
    }
}