/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NamedCaffeineCache<K, V> implements AsyncCache<K, V> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private AsyncCache<K, V> cache;
  private String name;
  private final Executor executor;

  public NamedCaffeineCache(String name, AsyncCache<K, V> cache) {
    this.cache = cache;
    this.name = name;
    executor = ForkJoinPool.commonPool();
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
  public @NonNull CompletableFuture<Map<K, V>> getAll(
      @NonNull Iterable<? extends K> keys,
      @NonNull Function<Iterable<? extends K>, Map<K, V>> mappingFunction) {
    return getAll(
        keys,
        (keysToLoad, executor) ->
            CompletableFuture.supplyAsync(() -> mappingFunction.apply(keysToLoad), executor));
  }

  @Override
  public @NonNull CompletableFuture<Map<K, V>> getAll(
      @NonNull Iterable<? extends K> keys,
      @NonNull BiFunction<Iterable<? extends K>, Executor, CompletableFuture<Map<K, V>>> mappingFunction) {
      Map<K, CompletableFuture<V>> existingFutureEntries = new HashMap<>();
      HashSet<K> missingEntryKeys = new HashSet<>();
      for (K key : keys) {
          if (!existingFutureEntries.containsKey(key)) {
              CompletableFuture<V> resultFuture = cache.getIfPresent(key);
              if (resultFuture == null) {
                  missingEntryKeys.add(key);
              } else {
                  existingFutureEntries.put(key, resultFuture);
              }
          }
      }
      CompletableFuture<Map<K, V>> existingEntriesFuture = combineFutureEntries(existingFutureEntries);
      if (missingEntryKeys.isEmpty()) {
          return existingEntriesFuture;
      } else {
          CompletableFuture<Map<K, V>> missingEntriesFuture = mappingFunction
              .apply(missingEntryKeys, executor)
              .whenComplete((map, error) -> {
                  if (map == null) {
                      if (error == null) {
                          error = new CompletionException("null map", null);
                      }
                      logger.warn("Exception thrown during asynchronous load of missing entries with keys: " + missingEntryKeys, error);
                  } else {
                      map.entrySet().forEach(e -> cache.asMap().putIfAbsent(e.getKey(), CompletableFuture.completedFuture(e.getValue())));
                  }
              });
          return CompletableFuture.allOf(existingEntriesFuture, missingEntriesFuture)
              .thenApply(vv -> {
                  Map<K, V> map = existingEntriesFuture.join();
                  missingEntriesFuture.join().forEach((k, v) -> map.putIfAbsent(k, v));
                  return map;
              });
      }
  }

  private CompletableFuture<Map<K, V>> combineFutureEntries(Map<K, CompletableFuture<V>> futureEntries) {
      return CompletableFuture.allOf(futureEntries.values().toArray(new CompletableFuture[futureEntries.size()]))
          .thenApply(ignored -> futureEntries
                      .entrySet()
                      .stream()
                      .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().join()))
                      .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))
          );
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
