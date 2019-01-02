/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.typesafe.config.Config;
import play.inject.Injector;
import play.libs.Scala;

/**
 * A Play application.
 * <p>
 * Application creation is handled by the framework engine.
 */
public interface Application {

    /**
     * Get the underlying Scala application.
     *
     * @return the application
     * @see Application#asScala() method
     *
     * @deprecated Use {@link #asScala()} instead.
     */
    @Deprecated
    play.api.Application getWrappedApplication();

    /**
     * Get the application as a Scala application.
     *
     * @return this application as a Scala application.
     * @see play.api.Application
     */
    play.api.Application asScala();

    /**
     * Get the application environment.
     *
     * @return the environment.
     */
    Environment environment();

    /**
     * Get the application configuration.
     *
     * @return the configuration
     */
    Config config();

    /**
     * Get the runtime injector for this application. In a runtime dependency injection based application, this can be
     * used to obtain components as bound by the DI framework.
     *
     * @return the injector
     */
    Injector injector();

    /**
     * Get the application path.
     *
     * @return the application path
     */
    default File path() {
        return asScala().path();
    }

    /**
     * Get the application classloader.
     *
     * @return the application classloader
     */
    default ClassLoader classloader() {
        return asScala().classloader();
    }

    /**
     * Check whether the application is in {@link Mode#DEV} mode.
     *
     * @return true if the application is in DEV mode
     */
    default boolean isDev() {
        return asScala().isDev();
    }

    /**
     * Check whether the application is in {@link Mode#PROD} mode.
     *
     * @return true if the application is in PROD mode
     */
    default boolean isProd() {
        return asScala().isProd();
    }

    /**
     * Check whether the application is in {@link Mode#TEST} mode.
     *
     * @return true if the application is in TEST mode
     */
    default boolean isTest() {
        return asScala().isTest();
    }

}
