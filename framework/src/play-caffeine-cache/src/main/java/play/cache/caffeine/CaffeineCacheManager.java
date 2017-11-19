package play.cache.caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

public class CaffeineCacheManager {
    private final ConcurrentMap<String, NamedCaffeineCache> cacheMap = new ConcurrentHashMap<>(16);

    private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder().expireAfter(new Expiry<Object, Object>() {
        @Override
        public long expireAfterCreate(@Nonnull Object key, @Nonnull Object value, long currentTime) {
            return Long.MAX_VALUE;
        }

        @Override
        public long expireAfterUpdate(@Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
            return Long.MAX_VALUE;
        }

        @Override
        public long expireAfterRead(@Nonnull Object key, @Nonnull Object value, long currentTime, long currentDuration) {
            return Long.MAX_VALUE;
        }
    });

    public CaffeineCacheManager(String spec) {
        if (spec != null && !spec.isEmpty()) {
            cacheBuilder = cacheBuilder.from(spec);
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