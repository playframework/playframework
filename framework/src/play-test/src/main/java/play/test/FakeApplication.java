/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.test;

import java.io.File;
import java.util.Map;

import com.typesafe.config.Config;

import play.api.mvc.Handler;
import play.inject.Injector;
import play.libs.Scala;

/**
 * A fake application for testing.
 *
 * @deprecated as of 2.5.0. Use GuiceApplicationBuilder to build your application instance.
 */
@Deprecated
public class FakeApplication implements play.Application {

    private final play.api.test.FakeApplication application;
    private final Config config;
    private final Injector injector;

    /**
     * Create a fake application.
     *
     * @param path application environment root path
     * @param classloader application environment class loader
     * @param additionalConfiguration additional configuration for the application
     */
    @SuppressWarnings("unchecked")
    public FakeApplication(
        File path,
        ClassLoader classloader,
        Map<String, ? extends Object> additionalConfiguration) {

        this.application = new play.api.test.FakeApplication(
            path,
            classloader,
            Scala.asScala((Map<String, Object>) additionalConfiguration),
            scala.PartialFunction$.MODULE$.<scala.Tuple2<String, String>, Handler>empty()
        );
        this.config = application.injector().instanceOf(Config.class);
        this.injector = application.injector().instanceOf(Injector.class);
    }

    /**
     * Get the underlying Scala application.
     */
    public play.api.test.FakeApplication getWrappedApplication() {
        return application;
    }

    /**
     * Get the application configuration.
     */
    public Config config() {
        return config;
    }

    /**
     * Get the application configuration.
     *
     * @deprecated Use config
     */
    @Deprecated
    public play.Configuration configuration() {
        return new play.Configuration(config);
    }

    /**
     * Get the injector for this application.
     */
    public Injector injector() {
        return injector;
    }

}
