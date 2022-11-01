/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

/** A synchronous API to access a Cache. */
public interface SyncCacheApi {
  /**
   * Retrieves an object by key.
   *
   * @param <T> the type of the stored object
   * @param key the key to look up
   * @return the object wrapped in an Optional
   */
  <T> Optional<T> get(String key);

  /**
   * Retrieves an object by key.
   *
   * @param <T> the type of the stored object
   * @param key the key to look up
   * @return the object wrapped in an Optional
   * @deprecated Deprecated as of 2.8.0. Renamed to {@link #get(String)}.
   */
  @Deprecated
  default <T> Optional<T> getOptional(String key) {
    return get(key);
  }

  /**
   * Retrieve a value from the cache, or set it from a default Callable function.
   *
   * @param <T> the type of the value
   * @param key Item key.
   * @param block block returning value to set if key does not exist
   * @param expiration expiration period in seconds.
   * @return the value
   */
  <T> T getOrElseUpdate(String key, Callable<T> block, int expiration);

  /**
   * Retrieve a value from the cache, or set it from a default Callable function.
   *
   * @param <T> the type of the value
   * @param key Item key.
   * @param block block returning value to set if key does not exist
   * @param expiration function that returns expiration period in seconds.
   * @return the value
   */
  <T> T getOrElseUpdate(String key, Callable<T> block, Function<T, Integer> expiration);

  /**
   * Retrieve a value from the cache, or set it from a default Callable function.
   *
   * <p>The value has no expiration.
   *
   * @param <T> the type of the value
   * @param key Item key.
   * @param block block returning value to set if key does not exist
   * @return the value
   */
  <T> T getOrElseUpdate(String key, Callable<T> block);

  /**
   * Sets a value with expiration.
   *
   * @param key Item key.
   * @param value The value to set.
   * @param expiration expiration in seconds
   */
  void set(String key, Object value, int expiration);

  /**
   * Sets a value without expiration.
   *
   * @param key Item key.
   * @param value The value to set.
   */
  void set(String key, Object value);

  /**
   * Removes a value from the cache.
   *
   * @param key The key to remove the value for.
   */
  void remove(String key);
}
