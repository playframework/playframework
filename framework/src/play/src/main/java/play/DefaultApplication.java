/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import javax.inject.Inject;
import javax.inject.Singleton;
import play.inject.Injector;

/**
 * Default implementation of a Play Application.
 *
 * Application creation is handled by the framework engine.
 */
@Singleton
public class DefaultApplication implements Application {

    private final play.api.Application application;
    private final Configuration configuration;
    private final Injector injector;

    /**
     * Create an application that wraps a Scala application.
     *
     * @param application the application to wrap
     * @param configuration the new application's configuration
     * @param injector the new application's injector
     */
    @Inject
    public DefaultApplication(play.api.Application application, Configuration configuration, Injector injector) {
        this.application = application;
        this.configuration = configuration;
        this.injector = injector;
    }

    /**
     * Create an application that wraps a Scala application.
     *
     * @param application the application to wrap
     * @param injector the new application's injector
     */
    public DefaultApplication(play.api.Application application, Injector injector) {
        this(application, new Configuration(application.configuration()), injector);
    }

    /**
     * Get the underlying Scala application.
     *
     * @return the underlying application
     */
    public play.api.Application getWrappedApplication() {
      return application;
    }

    /**
     * Get the application configuration.
     *
     * @return the configuration
     */
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Get the injector for this application.
     *
     * @return the injector
     */
    public Injector injector() {
        return injector;
    }

}
