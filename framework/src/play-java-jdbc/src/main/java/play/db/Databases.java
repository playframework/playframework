/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Creation helpers for manually instantiating databases.
 */
public final class Databases {
// Databases is a final class and not an interface because an interface cannot be declared final. Also, that should
// clarify why the class' constructor is private: we really don't want this class to be either instantiated or subclassed.
	private Databases() {}
    // ----------------
    // Creation helpers
    // ----------------

    /**
     * Create a pooled database with the given configuration.
     *
     * @param name the database name
     * @param driver the database driver class
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured database
     */
    public static Database createFrom(String name, String driver, String url, Map<String, ? extends Object> config) {
        ImmutableMap.Builder<String, Object> dbConfig = new ImmutableMap.Builder<String, Object>();
        dbConfig.put("driver", driver);
        dbConfig.put("url", url);
        dbConfig.putAll(config);
        return new DefaultDatabase(name, dbConfig.build());
    }

    /**
     * Create a pooled database with the given configuration.
     *
     * @param name the database name
     * @param driver the database driver class
     * @param url the database url
     * @return a configured database
     */
    public static Database createFrom(String name, String driver, String url) {
        return createFrom(name, driver, url, ImmutableMap.<String, Object>of());
    }

    /**
     * Create a pooled database named "default" with the given configuration.
     *
     * @param driver the database driver class
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured database
     */
    public static Database createFrom(String driver, String url, Map<String, ? extends Object> config) {
        return createFrom("default", driver, url, config);
    }

    /**
     * Create a pooled database named "default" with the given driver and url.
     *
     * @param driver the database driver class
     * @param url the database url
     * @return a configured database
     */
    public static Database createFrom(String driver, String url) {
        return createFrom("default", driver, url, ImmutableMap.<String, Object>of());
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param url the database url
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, String url, Map<String, ? extends Object> config) {
        return createFrom(name, "org.h2.Driver", url, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param urlOptions a map of extra url options
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, Map<String, String> urlOptions, Map<String, ? extends Object> config) {
        String urlExtra = "";
        for (Map.Entry<String, String> option : urlOptions.entrySet()) {
            urlExtra += ";" + option.getKey() + "=" + option.getValue();
        }
        String url = "jdbc:h2:mem:" + name + urlExtra;
        return inMemory(name, url, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name, Map<String, ? extends Object> config) {
        return inMemory(name, "jdbc:h2:mem:" + name, config);
    }

    /**
     * Create an in-memory H2 database.
     *
     * @param name the database name
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(String name) {
        return inMemory(name, ImmutableMap.<String, Object>of());
    }

    /**
     * Create an in-memory H2 database with name "default".
     *
     * @param config a map of extra database configuration
     * @return a configured in-memory h2 database
     */
    public static Database inMemory(Map<String, ? extends Object> config) {
        return inMemory("default", config);
    }

    /**
     * Create an in-memory H2 database with name "default".
     *
     * @return a configured in-memory h2 database
     */
    public static Database inMemory() {
        return inMemory("default");
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     *
     * @param k1 an H2 configuration key.
     * @param v1 configuration value corresponding to `k1`
     * @return a configured in-memory H2 database
     */
    public static Database inMemoryWith(String k1, Object v1) {
        return inMemory(ImmutableMap.of(k1, v1));
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     *
     * @param k1 an H2 configuration key
     * @param v1 H2 configuration value corresponding to `k1`
     * @param k2 a second H2 configuration key
     * @param v2 a configuration value corresponding to `k2`
     * @return a configured in-memory H2 database
     */
    public static Database inMemoryWith(String k1, Object v1, String k2, Object v2) {
        return inMemory(ImmutableMap.of(k1, v1, k2, v2));
    }

    /**
     * Create an in-memory H2 database with name "default" and with
     * extra configuration provided by the given entries.
     *
     * @param k1 an H2 configuration key
     * @param v1 H2 configuration value corresponding to `k1`
     * @param k2 a second H2 configuration key
     * @param v2 a configuration value corresponding to `k2`
     * @param k3 a third H2 configuration key
     * @param v3 a configuration value corresponding to `k3`
     * @return a configured in-memory H2 database
     */
    public static Database inMemoryWith(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return inMemory(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
    }
}
