/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
  public CompletableFuture<V> getIfPresent(@Nonnull K key) {
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
      @Nonnull
          BiFunction<? super K, ? super Executor, ? extends CompletableFuture<? extends V>>
              mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public @Nonnull CompletableFuture<Map<K, V>> getAll(
      @Nonnull Iterable<? extends K> keys,
      @Nonnull
          Function<? super Set<? extends K>, ? extends Map<? extends K, ? extends V>>
              mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public @Nonnull CompletableFuture<Map<K, V>> getAll(
      @Nonnull Iterable<? extends K> keys,
      @Nonnull
          BiFunction<
                  ? super Set<? extends K>,
                  ? super Executor,
                  ? extends CompletableFuture<? extends Map<? extends K, ? extends V>>>
              mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public void put(@Nonnull K key, @Nonnull CompletableFuture<? extends V> value) {
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
