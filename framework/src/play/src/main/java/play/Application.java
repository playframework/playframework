/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
     */
    play.api.Application getWrappedApplication();

    /**
     * Get the application configuration.
     */
    Configuration configuration();

    /**
     * Get the injector for this application.
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
     * Get the {@link play.Plugin} instance for the given class.
     *
     * @param pluginClass the Class of the plugin
     * @return an instance of the plugin (if found, otherwise null)
     */
    default <T> T plugin(Class<T> pluginClass) {
        return Scala.orNull(getWrappedApplication().plugin(pluginClass));
    }

    /**
     * Check whether the application is in {@link Mode#DEV} mode.
     *
     * @return true if the application is in DEV mode
     */
    default boolean isDev() {
        return play.api.Play.isDev(getWrappedApplication());
    }

    /**
     * Check whether the application is in {@link Mode#PROD} mode.
     *
     * @return true if the application is in PROD mode
     */
    default boolean isProd() {
        return play.api.Play.isProd(getWrappedApplication());
    }

    /**
     * Check whether the application is in {@link Mode#TEST} mode.
     *
     * @return true if the application is in TEST mode
     */
    default boolean isTest() {
        return play.api.Play.isTest(getWrappedApplication());
    }

}
