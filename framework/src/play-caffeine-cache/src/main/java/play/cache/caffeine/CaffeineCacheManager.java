package play.cache.caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

public class CaffeineCacheManager {
    private final ConcurrentMap<String, NamedCaffeineCache> cacheMap = new ConcurrentHashMap<>(16);

    private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

    public CaffeineCacheManager(Config config) {
        if (config != null) {
            cacheBuilder = CaffeineParser.from(config).expireAfter(new CaffeineDefaultExpiry());
        } else {
            cacheBuilder = Caffeine.newBuilder().expireAfter(new CaffeineDefaultExpiry());
        }
    }

    public <K, V> NamedCaffeineCache<K, V> getCache(String cacheName) {
        NamedCaffeineCache<K, V> namedCache = cacheMap.getOrDefault(cacheName, null);

        if (namedCache == null) {
            namedCache = new NamedCaffeineCache(cacheName, cacheBuilder.build());
            cacheMap.put(cacheName, namedCache);
        }

        return namedCache;
    }
}