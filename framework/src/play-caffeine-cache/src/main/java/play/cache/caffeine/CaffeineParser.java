/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

import java.util.Map;
import java.util.Objects;

/**
 * A configuration parser for the {@link Caffeine} builder.
 * <p>
 * <ul>
 *   <li>{@code initial-capacity=[integer]}: sets {@link Caffeine#initialCapacity}.
 *   <li>{@code maximum-size=[long]}: sets {@link Caffeine#maximumSize}.
 *   <li>{@code weak-keys}=[condition]: sets {@link Caffeine#weakKeys}.
 *   <li>{@code weak-values}=[condition]: sets {@link Caffeine#weakValues}.
 *   <li>{@code soft-values}=[condition]: sets {@link Caffeine#softValues}.
 *   <li>{@code record-stats}=[condition]: sets {@link Caffeine#recordStats}.
 * </ul>
 * It is illegal to use the following configurations together:
 * <ul>
 *   <li>{@code maximumSize} and {@code maximumWeight}
 *   <li>{@code weakValues} and {@code softValues} set to {@code true}
 * </ul>
 * <p>
 * {@code CaffeineParser} does not support configuring {@code Caffeine} methods with non-value
 * parameters. These must be configured in code.
 */
public final class CaffeineParser {
    private final Caffeine<Object, Object> cacheBuilder;
    private final Config config;

    private CaffeineParser(Config config) {
        this.cacheBuilder = Caffeine.newBuilder();
        this.config = Objects.requireNonNull(config);
    }

    /** Returns a configured {@link Caffeine} cache builder. */
    public static Caffeine<Object, Object> from(Config config) {
        CaffeineParser parser = new CaffeineParser(config);
        config.entrySet().stream().map(Map.Entry::getKey).forEach(parser::parse);
        return parser.cacheBuilder;
    }

    private void parse(String key) {
        switch (key) {
            case "initial-capacity":
                if (!config.getIsNull(key)) {
                    cacheBuilder.initialCapacity(config.getInt(key));
                }
                break;
            case "maximum-size":
                if (!config.getIsNull(key)) {
                    cacheBuilder.maximumSize(config.getLong(key));
                }
                break;
            case "weak-keys":
                conditionally(key, cacheBuilder::weakKeys);
                break;
            case "weak-values":
                conditionally(key, cacheBuilder::weakValues);
                break;
            case "soft-values":
                conditionally(key, cacheBuilder::softValues);
                break;
            case "record-stats":
                conditionally(key, cacheBuilder::recordStats);
                break;
            default:
                break;
        }
    }
    private void conditionally(String key, Runnable action) {
        if (config.getBoolean(key)) {
            action.run();
        }
    }
}