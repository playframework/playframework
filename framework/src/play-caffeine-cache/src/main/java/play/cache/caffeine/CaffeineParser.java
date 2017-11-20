package play.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A configuration parser for the {@link Caffeine} builder.
 * <p>
 * <ul>
 *   <li>{@code initial-capacity=[integer]}: sets {@link Caffeine#initialCapacity}.
 *   <li>{@code maximum-size=[long]}: sets {@link Caffeine#maximumSize}.
 *   <li>{@code maximum-weight=[memory-size]}: sets {@link Caffeine#maximumWeight}.
 *   <li>{@code expire-after-access=[duration]}: sets {@link Caffeine#expireAfterAccess}.
 *   <li>{@code expire-after-write=[duration]}: sets {@link Caffeine#expireAfterWrite}.
 *   <li>{@code refresh-after-write=[duration]}: sets {@link Caffeine#refreshAfterWrite}.
 *   <li>{@code weak-keys}=[condition]: sets {@link Caffeine#weakKeys}.
 *   <li>{@code weak-values}=[condition]: sets {@link Caffeine#weakValues}.
 *   <li>{@code soft-values}=[condition]: sets {@link Caffeine#softValues}.
 *   <li>{@code record-stats}=[condition]: sets {@link Caffeine#recordStats}.
 * </ul>
 * <p>
 * The maximum weight may be specified numerically or as a memory size. When set using a memory size
 * syntax the builder is set to the number of bytes.
 * <p>
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

    @SuppressWarnings("fallthrough")
    private void parse(String key) {
        switch (key) {
            case "initial-capacity":
                System.out.println("initial-capacity:" + config.getInt(key));
                cacheBuilder.initialCapacity(config.getInt(key));
                break;
            case "maximum-size":
                System.out.println("maximum-size:" + config.getLong(key));
                cacheBuilder.maximumSize(config.getLong(key));
                break;
            case "maximum-weight":
                System.out.println("maximum-weight:" + config.getMemorySize(key));
                cacheBuilder.maximumWeight(config.getMemorySize(key).toBytes());
                break;
            case "expire-after-access":
                System.out.println("expire-after-access:" + getNanos(key));
                cacheBuilder.expireAfterAccess(getNanos(key), TimeUnit.NANOSECONDS);
                break;
            case "expire-after-write":
                System.out.println("xpire-after-write:" + getNanos(key));
                cacheBuilder.expireAfterWrite(getNanos(key), TimeUnit.NANOSECONDS);
                break;
            case "refreshAfterWrite":
                System.out.println("refreshAfterWrite:" + getNanos(key));
                cacheBuilder.refreshAfterWrite(getNanos(key), TimeUnit.NANOSECONDS);
                break;
            case "weak-keys":
                System.out.println("weak-keys:" + config.getBoolean(key));
                conditionally(key, cacheBuilder::weakKeys);
                break;
            case "weak-values":
                System.out.println("weak-values:" + config.getBoolean(key));
                conditionally(key, cacheBuilder::weakValues);
                break;
            case "soft-values":
                System.out.println("soft-values:" + config.getBoolean(key));
                conditionally(key, cacheBuilder::softValues);
                break;
            case "record-stats":
                System.out.println("record-stats:" + config.getBoolean(key));
                conditionally(key, cacheBuilder::recordStats);
                break;
            default:
                break;
        }
    }

    private long getNanos(String key) {
        return config.getDuration(key, TimeUnit.NANOSECONDS);
    }

    private void conditionally(String key, Runnable action) {
        if (config.getBoolean(key)) {
            action.run();
        }
    }
}