/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import play.inject.Injector;
import play.libs.Scala;

/**
 * A Play application.
 *
 * Application creation is handled by the framework engine.
 */
public interface Application {

    /**
     * Get the underlying Scala application.
     *
     * @return the application
     */
    play.api.Application getWrappedApplication();

    /**
     * Get the application configuration.
     *
     * @return the configuration
     */
    Configuration configuration();

    /**
     * Get the injector for this application.
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
        return getWrappedApplication().path();
    }

    /**
     * Get the application classloader.
     *
     * @return the application classloader
     */
    default ClassLoader classloader() {
        return getWrappedApplication().classloader();
    }

    /**
     * Get a file relative to the application root path.
     *
     * @param relativePath relative path of the file to fetch
     * @return a file instance - it is not guaranteed that the file exists
     */
    default File getFile(String relativePath) {
        return getWrappedApplication().getFile(relativePath);
    }

    /**
     * Get a resource from the classpath.
     *
     * @param relativePath relative path of the resource to fetch
     * @return URL to the resource (may be null)
     */
    default URL resource(String relativePath) {
        return Scala.orNull(getWrappedApplication().resource(relativePath));
    }

    /**
     * Get a resource stream from the classpath.
     *
     * @param relativePath relative path of the resource to fetch
     * @return InputStream to the resource (may be null)
     */
    default InputStream resourceAsStream(String relativePath) {
        return Scala.orNull(getWrappedApplication().resourceAsStream(relativePath));
    }

    /**
     * Check whether the application is in {@link Mode#DEV} mode.
     *
     * @return true if the application is in DEV mode
     */
    default boolean isDev() { return getWrappedApplication().isDev(); }

    /**
     * Check whether the application is in {@link Mode#PROD} mode.
     *
     * @return true if the application is in PROD mode
     */
    default boolean isProd() {
        return getWrappedApplication().isProd();
    }

    /**
     * Check whether the application is in {@link Mode#TEST} mode.
     *
     * @return true if the application is in TEST mode
     */
    default boolean isTest() {
        return getWrappedApplication().isTest();
    }

}
