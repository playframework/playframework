/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.cache;

import java.util.concurrent.Callable;

/**
 * Provides an access point for Play's cache service.
 */
public class Cache {

    private static CacheApi cacheApi() {
        return play.Play.application().injector().instanceOf(CacheApi.class);
    }

    /**
     * Retrieves an object by key.
     *
     * @return object
     */
    public static Object get(String key) {
        return cacheApi().get(key);
    }

    /**
     * Retrieve a value from the cache, or set it from a default Callable function.
     *
     * @param key        Item key.
     * @param block      block returning value to set if key does not exist
     * @param expiration expiration period in seconds.
     * @return value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrElse(String key, Callable<T> block, int expiration) {
        return cacheApi().getOrElse(key, block, expiration);
    }

    /**
     * Sets a value with expiration.
     *
     * @param expiration expiration in seconds
     */
    public static void set(String key, Object value, int expiration) {
        cacheApi().set(key, value, expiration);
    }

    /**
     * Sets a value without expiration.
     */
    public static void set(String key, Object value) {
        cacheApi().set(key, value);
    }

    public static void remove(String key) {
        cacheApi().remove(key);
    }
}
