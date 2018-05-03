/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import com.typesafe.config.Config;
import org.slf4j.ILoggerFactory;
import play.api.Configuration;
import play.api.LoggerConfigurator$;
import play.libs.Scala;
import scala.Option;
import scala.compat.java8.OptionConverters;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional ;

/**
 * Runs through underlying logger configuration.
 */
public interface LoggerConfigurator extends play.api.LoggerConfigurator {

    /**
     * Initialize the Logger when there's no application ClassLoader available.
     * @param rootPath the root path
     * @param mode the ode
     */
    void init(File rootPath, Mode mode);

    @Override
    default void init(File rootPath, play.api.Mode mode) {
        init(rootPath, mode.asJava());
    }

    /**
     * This is a convenience method that adds no extra properties.
     * @param env the environment.
     */
    void configure(Environment env);

    @Override
    default void configure(play.api.Environment env) {
        configure(env.asJava());
    }

    /**
     * Configures the logger with the environment and the application configuration.
     * <p>
     * This is what full applications will run, and the place to put extra properties,
     * either through optionalProperties or by setting configuration properties and
     * having "play.logger.includeConfigProperties=true" in the config.
     *
     * @param env                the application environment
     * @param configuration      the application's configuration
     */
    default void configure(Environment env, Config configuration) {
        configure(env, configuration, Collections.emptyMap());
    }

    /**
     * Configures the logger with the environment, the application configuration and
     * additional properties.
     * <p>
     * This is what full applications will run, and the place to put extra properties,
     * either through optionalProperties or by setting configuration properties and
     * having "play.logger.includeConfigProperties=true" in the config.
     *
     * @param env                the application environment
     * @param configuration      the application's configuration
     * @param optionalProperties any optional properties (you can use an empty Map otherwise)
     */
    void configure(Environment env, Config configuration, Map<String, String> optionalProperties);

    @Override
    default void configure(play.api.Environment env, Configuration configuration, scala.collection.immutable.Map<String, String> optionalProperties) {
        configure(
            env.asJava(),
            configuration.underlying(),
            Scala.asJava(optionalProperties)
        );
    }

    /**
     * Configures the logger with a list of properties and an optional URL.
     * <p>
     * This is the engine's entrypoint method that has all the properties pre-assembled.
     * @param properties the properties
     * @param config the configuration URL
     */
    void configure(Map<String, String> properties, Optional<URL> config);

    @Override
    default void configure(scala.collection.immutable.Map<String, String> properties, Option<URL> config) {
        configure(
            Scala.asJava(properties),
            OptionConverters.toJava(config)
        );
    }

    /**
     * Returns the logger factory for the configurator.  Only safe to call after configuration.
     *
     * @return an instance of ILoggerFactory
     */
    ILoggerFactory loggerFactory();

    /**
     * Shutdown the logger infrastructure.
     */
    void shutdown();

    static Optional<LoggerConfigurator> apply(ClassLoader classLoader) {
        return OptionConverters
            .toJava(LoggerConfigurator$.MODULE$.apply(classLoader))
            .map(loggerConfigurator -> {
                if (loggerConfigurator instanceof LoggerConfigurator) {
                    return (LoggerConfigurator)loggerConfigurator;
                } else {
                    // Avoid failing if using a Scala logger configurator
                    return new DelegateLoggerConfigurator(loggerConfigurator);
                }
            });
    }

    static Map<String, String> generateProperties(Environment env, Config config, Map<String, String> optionalProperties) {
        scala.collection.immutable.Map<String, String> generateProperties = LoggerConfigurator$.MODULE$.generateProperties(
                env.asScala(),
                new Configuration(config),
                Scala.asScala(optionalProperties)
        );
        return Scala.asJava(generateProperties);
    }
}
