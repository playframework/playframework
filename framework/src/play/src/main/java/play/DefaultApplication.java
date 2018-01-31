/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.typesafe.config.Config;
import play.inject.Injector;

/**
 * Default implementation of a Play Application.
 *
 * Application creation is handled by the framework engine.
 */
@Singleton
public class DefaultApplication implements Application {

    private final play.api.Application application;
    private final Config config;
    private final Injector injector;

    /**
     * Create an application that wraps a Scala application.
     *
     * @param application the application to wrap
     * @param config the new application's configuration
     * @param injector the new application's injector
     */
    @Inject
    public DefaultApplication(play.api.Application application, Config config, Injector injector) {
        this.application = application;
        this.config = config;
        this.injector = injector;
    }

    /**
     * Create an application that wraps a Scala application.
     *
     * @param application the application to wrap
     * @param injector the new application's injector
     */
    public DefaultApplication(play.api.Application application, Injector injector) {
        this(application, application.configuration().underlying(), injector);
    }

    /**
     * Get the underlying Scala application.
     *
     * @return the underlying application
     */
    @Override
    public play.api.Application getWrappedApplication() {
      return application;
    }

    /**
     * Get the application as a Scala application.
     *
     * @see play.api.Application
     */
    @Override
    public play.api.Application asScala() {
        return application;
    }

    /**
     * Get the application configuration.
     *
     * @return the configuration
     */
    @Override
    public Config config() {
        return config;
    }

    /**
     * Get the injector for this application.
     *
     * @return the injector
     */
    @Override
    public Injector injector() {
        return injector;
    }

}
