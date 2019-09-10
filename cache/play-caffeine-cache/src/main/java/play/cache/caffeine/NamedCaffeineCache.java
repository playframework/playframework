/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
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
  public @NonNull CompletableFuture<V> get(
      @NonNull K key,
      @NonNull BiFunction<? super K, Executor, CompletableFuture<V>> mappingFunction) {
    return cache.get(key, mappingFunction);
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
