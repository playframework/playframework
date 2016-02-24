/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.test;

import java.io.File;
import java.util.Map;

import play.api.mvc.Handler;
import play.Configuration;
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
    private final Configuration configuration;
    private final Injector injector;

    /**
     * Create a fake application.
     *
     * @deprecated Use the version without GlobalSettings, since 2.5.0
     *
     * @param path application environment root path
     * @param classloader application environment class loader
     * @param additionalConfiguration additional configuration for the application
     * @param global global settings to use in place of default global
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public FakeApplication(
        File path,
        ClassLoader classloader,
        Map<String, ? extends Object> additionalConfiguration,
        play.GlobalSettings global) {

        play.api.GlobalSettings scalaGlobal = (global != null) ? new play.core.j.JavaGlobalSettingsAdapter(global) : null;

        this.application = new play.api.test.FakeApplication(
            path,
            classloader,
            Scala.asScala((Map<String, Object>) additionalConfiguration),
            scala.Option.apply(scalaGlobal),
            scala.PartialFunction$.MODULE$.<scala.Tuple2<String, String>, Handler>empty()
        );
        this.configuration = application.injector().instanceOf(Configuration.class);
        this.injector = application.injector().instanceOf(Injector.class);
    }

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
        this(path, classloader, additionalConfiguration, null);
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
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Get the injector for this application.
     */
    public Injector injector() {
        return injector;
    }

}
