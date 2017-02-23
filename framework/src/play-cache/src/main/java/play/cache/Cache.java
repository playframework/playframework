/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.cache;

import java.util.concurrent.Callable;

/**
 * Provides an access point for Play's cache service.
 *
 * @deprecated Please use an dependency injected instance of CacheApi.
 */
@Deprecated
public class Cache {

    static CacheApi cacheApi() {
        return play.api.Play.current().injector().instanceOf(CacheApi.class);
    }

    /**
     * Retrieves an object by key.
     *
     * @param key the cache key
     * @return the object or null
     * @deprecated Please use non-static cacheApi.get(key), deprecated since 2.5.0
     */
    @Deprecated
    public static Object get(String key) {
        return cacheApi().get(key);
    }

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @deprecated Please use non-static cacheApi.getOrElse, deprecated since 2.5.0
     *
     * @param <T>        the type of object being queried
     * @param key        Item key.
     * @param block      block returning value to set if key does not exist
     * @param expiration expiration period in seconds.
     * @return value
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T getOrElse(String key, Callable<T> block, int expiration) {
        return cacheApi().getOrElse(key, block, expiration);
    }

    /**
     * Sets a value with expiration.
     *
     * @deprecated Please use non-static cacheApi.set(key,value,expiration), deprecated since 2.5.0
     *
     * @param key the key to set
     * @param value the value to set
     * @param expiration expiration in seconds
     */
    @Deprecated
    public static void set(String key, Object value, int expiration) {
        cacheApi().set(key, value, expiration);
    }

    /**
     * Sets a value without expiration.
     *
     * @param key the key to set
     * @param value the value to set
     */
    @Deprecated
    public static void set(String key, Object value) {
        cacheApi().set(key, value);
    }

    /**
     * Removes the entry at a specific key
     *
     * @deprecated Please use non-static cacheApi.set(key,value,expiration), deprecated since 2.5.0
     *
     * @param key the key whose entry to remove
     */
    @Deprecated
    public static void remove(String key) {
        cacheApi().remove(key);
    }
}
