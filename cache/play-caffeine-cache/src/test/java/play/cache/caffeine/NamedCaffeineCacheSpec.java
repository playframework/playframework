/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class NamedCaffeineCacheSpec {

  private NamedCaffeineCache<String, String> cache =
      new NamedCaffeineCache<>("testNamedCaffeineCache", Caffeine.newBuilder().buildAsync());

  @Test
  public void getAll_shouldReturnAllValuesWithTheGivenKeys() throws Exception {
    String key1 = "key1";
    String value1 = "value1";
    String key2 = "key2";
    String value2 = "value2";
    cache.put(key1, CompletableFuture.completedFuture(value1));
    cache.put(key2, CompletableFuture.completedFuture(value2));
    Set<String> keys = new HashSet<>(Arrays.asList(key1, key2));

    CompletableFuture<Map<String, String>> futureResult =
        cache.getAll(
            keys, (missingKeys, executor) -> CompletableFuture.completedFuture(new HashMap<>()));
    Map<String, String> resultMap = futureResult.get(2, TimeUnit.SECONDS);
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(key1, value1);
    expectedMap.put(key2, value2);

    assertThat(resultMap, equalTo(expectedMap));
  }

  @Test
  public void getAll_shouldCreateTheMissingValuesAndReturnAllWithTheGivenKeys() throws Exception {
    String key1 = "key1";
    String value1 = "value1";
    String key2 = "key2";
    String value2 = "value2";
    cache.put(key1, CompletableFuture.completedFuture(value1));
    Set<String> keys = new HashSet<>(Arrays.asList(key1, key2));
    HashMap<String, String> missingValuesMap = new HashMap<>();
    missingValuesMap.put(key2, value2);

    CompletableFuture<Map<String, String>> futureResult =
        cache.getAll(
            keys, (missingKeys, executor) -> CompletableFuture.completedFuture(missingValuesMap));
    Map<String, String> resultMap = futureResult.get(2, TimeUnit.SECONDS);
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(key1, value1);
    expectedMap.put(key2, value2);

    assertThat(resultMap, equalTo(expectedMap));
  }

  @Test
  public void getAll_shouldNotReplaceAlreadyExistingValues() throws Exception {
    String key1 = "key1";
    String value1 = "value1";
    String key2 = "key2";
    String value2 = "value2";
    cache.put(key1, CompletableFuture.completedFuture(value1));
    Set<String> keys = new HashSet<>(Arrays.asList(key1, key2));
    HashMap<String, String> missingValuesMap = new HashMap<>();
    missingValuesMap.put(key2, value2);
    missingValuesMap.put(key1, "value3"); // "value1" should not be replaced with "value3"

    CompletableFuture<Map<String, String>> futureResult =
        cache.getAll(
            keys, (missingKeys, executor) -> CompletableFuture.completedFuture(missingValuesMap));
    Map<String, String> resultMap = futureResult.get(2, TimeUnit.SECONDS);
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(key1, value1);
    expectedMap.put(key2, value2);

    assertThat(resultMap, equalTo(expectedMap));
  }

  @Test()
  public void getAll_shouldReturnFailedFutureIfMappingFunctionIsCompletedExceptionally()
      throws Exception {
    LoggerFactory.getLogger(NamedCaffeineCache.class);
    RuntimeException testException = new RuntimeException("test exception");
    CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
    future.completeExceptionally(testException);
    CompletableFuture<Map<String, String>> resultFuture =
        cache.getAll(new HashSet<>(Arrays.asList("key1")), (missingKeys, executor) -> future);
    assertThat(resultFuture.isCompletedExceptionally(), equalTo(true));
  }
}
