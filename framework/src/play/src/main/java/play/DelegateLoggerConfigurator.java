/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import com.typesafe.config.Config;
import org.slf4j.ILoggerFactory;
import play.libs.Scala;
import scala.compat.java8.OptionConverters;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

/**
 * Java delegator to encapsulates a {@link play.api.LoggerConfigurator}.
 */
class DelegateLoggerConfigurator implements LoggerConfigurator {

    private final play.api.LoggerConfigurator delegate;

    @Inject
    public DelegateLoggerConfigurator(play.api.LoggerConfigurator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init(File rootPath, Mode mode) {
        delegate.init(rootPath, mode.asScala());
    }

    @Override
    public void configure(Environment env) {
        delegate.configure(env.asScala());
    }

    @Override
    public void configure(Environment env, Config configuration, Map<String, String> optionalProperties) {
        delegate.configure(
            env.asScala(),
            new play.api.Configuration(configuration),
            Scala.asScala(optionalProperties)
        );
    }

    @Override
    public void configure(Map<String, String> properties, Optional<URL> config) {
        delegate.configure(
            Scala.asScala(properties),
            OptionConverters.toScala(config)
        );
    }

    @Override
    public ILoggerFactory loggerFactory() {
        return delegate.loggerFactory();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }
}
