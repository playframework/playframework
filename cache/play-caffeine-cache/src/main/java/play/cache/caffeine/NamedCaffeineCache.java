/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class NamedCaffeineCache<K, V> implements AsyncCache<K, V> {

  private AsyncCache<K, V> cache;
  private String name;

  public NamedCaffeineCache(String name, AsyncCache<K, V> cache) {
    this.cache = cache;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  @Override
  public CompletableFuture<V> getIfPresent(@Nonnull Object key) {
    return cache.getIfPresent(key);
  }

  @CheckForNull
  @Override
  public CompletableFuture<V> get(
      @Nonnull K key, @Nonnull Function<? super K, ? extends V> mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public @Nonnull CompletableFuture<V> get(
      @Nonnull K key,
      @Nonnull BiFunction<? super K, Executor, CompletableFuture<V>> mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public @Nonnull CompletableFuture<Map<K, V>> getAll(
      @Nonnull Iterable<? extends K> keys,
      @Nonnull Function<Iterable<? extends K>, Map<K, V>> mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public @Nonnull CompletableFuture<Map<K, V>> getAll(
      @Nonnull Iterable<? extends K> keys,
      @Nonnull
          BiFunction<Iterable<? extends K>, Executor, CompletableFuture<Map<K, V>>>
              mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public void put(@Nonnull K key, @Nonnull CompletableFuture<V> value) {
    cache.put(key, value);
  }

  @Nonnull
  @Override
  public ConcurrentMap<K, CompletableFuture<V>> asMap() {
    return cache.asMap();
  }

  @Override
  public Cache<K, V> synchronous() {
    return cache.synchronous();
  }
}
