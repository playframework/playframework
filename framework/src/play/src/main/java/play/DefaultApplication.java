/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
     */
    @Inject
    public DefaultApplication(play.api.Application application, Configuration configuration, Injector injector) {
        this.application = application;
        this.configuration = configuration;
        this.injector = injector;
    }

    /**
     * Create an application that wraps a Scala application.
     */
    public DefaultApplication(play.api.Application application, Injector injector) {
        this(application, new Configuration(application.configuration()), injector);
    }

    /**
     * Get the underlying Scala application.
     */
    public play.api.Application getWrappedApplication() {
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
