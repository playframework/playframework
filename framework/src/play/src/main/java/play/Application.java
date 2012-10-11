package play;

import java.io.*;
import java.util.*;
import java.net.*;

import play.libs.Scala;

/**
 * A Play application.
 * <p>
 * Application creation is handled by the framework engine.
 */
public class Application {
    
    private final play.api.Application application;
    
    public play.api.Application getWrappedApplication() {
      return application;
    }

    /**
     * Creates an application from a Scala Application value.
     */
    public Application(play.api.Application application) {
        this.application = application;
    }
    
    /**
     * Retrieves the application path.
     * <p>
     * @return the application path
     */
    public File path() {
        return application.path();
    }
    
    /**
     * Retrieves the application configuration/
     * <p>
     * @return the application path
     */
    public Configuration configuration() {
        return new Configuration(application.configuration());
    }
    
    /**
     * Retrieves the application classloader.
     * <p>
     * @return the application classloader
     */
    public ClassLoader classloader() {
        return application.classloader();
    }
    
    /**
     * Retrieves a file relative to the application root path.
     *
     * @param relativePath relative path of the file to fetch
     * @return a file instance - it is not guaranteed that the file exists
     */
    public File getFile(String relativePath) {
        return application.getFile(relativePath);
    }
    
    /**
     * Retrieves a resource from the classpath.
     *
     * @param relativePath relative path of the resource to fetch
     * @return URL to the resource (may be null)
     */
    public URL resource(String relativePath) {
        return Scala.orNull(application.resource(relativePath));
    }
    
    /**
     * Retrieves a resource stream from the classpath.
     *
     * @param relativePath relative path of the resource to fetch
     * @return InputStream to the resource (may be null)
     */
    public InputStream resourceAsStream(String relativePath) {
        return Scala.orNull(application.resourceAsStream(relativePath));
    }
    
    /**
     * Retrieve the plugin instance for the class.
     */
    public <T> T plugin(Class<T> pluginClass) {
        return Scala.orNull(application.plugin(pluginClass));
    }
    
    /**
     * Returns `true` if the application is `DEV` mode.
     */
    public boolean isDev() {
        return play.api.Play.isDev(application);
    }
    
    /**
     * Returns `true` if the application is `PROD` mode.
     */
    public boolean isProd() {
        return play.api.Play.isProd(application);
    }
    
    /**
     * Returns `true` if the application is `TEST` mode.
     */
    public boolean isTest() {
        return play.api.Play.isTest(application);
    }
    
}
