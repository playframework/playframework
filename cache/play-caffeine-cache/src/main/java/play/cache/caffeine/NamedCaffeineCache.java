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

  @Override
  public CompletableFuture<V> getIfPresent(K key) {
    return cache.getIfPresent(key);
  }

  @Override
  public CompletableFuture<V> get(K key, Function<? super K, ? extends V> mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public CompletableFuture<V> get(
      K key,
      BiFunction<? super K, ? super Executor, ? extends CompletableFuture<? extends V>>
          mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public CompletableFuture<Map<K, V>> getAll(
      Iterable<? extends K> keys,
      Function<? super Set<? extends K>, ? extends Map<? extends K, ? extends V>> mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public CompletableFuture<Map<K, V>> getAll(
      Iterable<? extends K> keys,
      BiFunction<
              ? super Set<? extends K>,
              ? super Executor,
              ? extends CompletableFuture<? extends Map<? extends K, ? extends V>>>
          mappingFunction) {
    return cache.getAll(keys, mappingFunction);
  }

  @Override
  public void put(K key, CompletableFuture<? extends V> value) {
    cache.put(key, value);
  }

  @Override
  public ConcurrentMap<K, CompletableFuture<V>> asMap() {
    return cache.asMap();
  }

  @Override
  public Cache<K, V> synchronous() {
    return cache.synchronous();
  }
}
