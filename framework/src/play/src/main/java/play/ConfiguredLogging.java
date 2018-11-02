/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import com.typesafe.config.Config;
import org.slf4j.ILoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public interface ConfiguredLogging
{
    Environment environment();

    default ILoggerFactory configureLoggerFactory(final Config configuration) {
        final Environment environment = environment();
        final ILoggerFactory loggerFactory = LoggerConfigurator.apply(environment.classLoader()).map(lc -> {
            lc.configure(environment, configuration);
            return lc.loggerFactory();
        }).orElseGet(org.slf4j.LoggerFactory::getILoggerFactory);

        if (shouldDisplayLoggerDeprecationMessage(configuration)) {
            loggerFactory
                    .getLogger("application")
                    .warn("Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.");
        }

        return loggerFactory;
    }

    default boolean shouldDisplayLoggerDeprecationMessage(final Config configuration) {
        return configuration.hasPath("logger") && isDeprecated(
                configuration.getAnyRef("logger"),
                Arrays.asList("DEBUG", "WARN", "ERROR", "INFO", "TRACE", "OFF")
        );
    }

    default boolean hasDeprecatedValue(final Map<String, Object> config, final Collection<String> deprecatedValues) {
        return config.values().stream().map(value -> isDeprecated(value, deprecatedValues)).findAny().orElse(false);
    }

    @SuppressWarnings("unchecked")
    default Boolean isDeprecated(Object value, Collection<String> deprecatedValues) {
        if (value instanceof String) {
            return deprecatedValues.contains(value);
        } else if (value instanceof Map) {
            return hasDeprecatedValue((Map<String, Object>) value, deprecatedValues);
        } else {
            return false;
        }
    }
}
