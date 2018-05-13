/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import akka.Done;

/**
 * The Cache API.
 */
public interface AsyncCacheApi {

    /**
     * @return a synchronous version of this cache, which can be used to make synchronous calls.
     */
    default SyncCacheApi sync() {
        return new DefaultSyncCacheApi(this);
    }

    /**
     * Retrieves an object by key.
     *
     * @param <T> the type of the stored object
     * @param key the key to look up
     * @return a CompletionStage containing the value
     */
    <T> CompletionStage<T> get(String key);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @param <T> the type of the value
     * @param key Item key.
     * @param block block returning value to set if key does not exist
     * @param expiration expiration period in seconds.
     * @return a CompletionStage containing the value
     */
    <T> CompletionStage<T> getOrElseUpdate(String key, Callable<CompletionStage<T>> block, int expiration);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * The value has no expiration.
     *
     * @param <T> the type of the value
     * @param key Item key.
     * @param block block returning value to set if key does not exist
     * @return a CompletionStage containing the value
     */
    <T> CompletionStage<T> getOrElseUpdate(String key, Callable<CompletionStage<T>> block);

    /**
     * Sets a value with expiration.
     *
     * @param key Item key.
     * @param value The value to set.
     * @param expiration expiration in seconds
     * @return a CompletionStage containing the value
     */
    CompletionStage<Done> set(String key, Object value, int expiration);

    /**
     * Sets a value without expiration.
     *
     * @param key Item key.
     * @param value The value to set.
     * @return a CompletionStage containing the value
     */
    CompletionStage<Done> set(String key, Object value);

    /**
     * Removes a value from the cache.
     *
     * @param key The key to remove the value for.
     * @return a CompletionStage containing the value
     */
    CompletionStage<Done> remove(String key);

    /**
     * Removes all values from the cache. This may be useful as an admin user operation if it is supported by your cache.
     *
     * @throws UnsupportedOperationException if this cache implementation does not support removing all values.
     * @return a CompletionStage containing either a Done when successful or an exception when unsuccessful.
     */
    CompletionStage<Done> removeAll();
}
