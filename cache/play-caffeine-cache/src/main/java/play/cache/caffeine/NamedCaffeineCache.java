/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class NamedCaffeineCache<K, V> implements Cache<K, V> {
    private Cache<K, V> cache;
    private String name;

    public NamedCaffeineCache(String name, Cache<K, V> cache) {
        this.cache = cache;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @CheckForNull
    @Override
    public V getIfPresent(@Nonnull Object key) {
        return cache.getIfPresent(key);
    }

    @CheckForNull
    @Override
    public V get(@Nonnull K key, @Nonnull Function<? super K, ? extends V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

    @Nonnull
    @Override
    public Map<K, V> getAllPresent(@Nonnull Iterable<?> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    public void put(@Nonnull K key, @Nonnull V value) {
        cache.put(key, value);
    }

    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends V> map) {
        cache.putAll(map);
    }

    @Override
    public void invalidate(@Nonnull Object key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll(@Nonnull Iterable<?> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }

    @Nonnull
    @Override
    public CacheStats stats() {
        return cache.stats();
    }

    @Nonnull
    @Override
    public ConcurrentMap<K, V> asMap() {
        return cache.asMap();
    }

    @Override
    public void cleanUp() {
        cache.cleanUp();
    }

    @Nonnull
    @Override
    public Policy<K, V> policy() {
        return cache.policy();
    }
}