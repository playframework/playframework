/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import java.util.concurrent.Callable;

/**
 * The Cache API.
 */
public interface CacheApi {
    /**
     * Retrieves an object by key.
     *
     * @return object
     */
    public <T> T get(String key);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @param key Item key.
     * @param block block returning value to set if key does not exist
     * @param expiration expiration period in seconds.
     * @return value
     */
    public <T> T getOrElse(String key, Callable<T> block, int expiration);

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * The value has no expiration.
     *
     * @param key Item key.
     * @param block block returning value to set if key does not exist
     * @return value
     */
    public <T> T getOrElse(String key, Callable<T> block);

    /**
     * Sets a value with expiration.
     *
     * @param key Item key.
     * @param value The value to set.
     * @param expiration expiration in seconds
     */
    public void set(String key, Object value, int expiration);

    /**
     * Sets a value without expiration.
     *
     * @param key Item key.
     * @param value The value to set.
     */
    public void set(String key, Object value);

    /**
     * Removes a value from the cache.
     *
     * @param key The key to remove the value for.
     */
    public void remove(String key);
}
