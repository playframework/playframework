package play;

import java.io.*;
import java.util.*;

/**
 * A Play application.
 *
 * Application creation is handled by the framework engine.
 */
public class Application {
    
    private final play.api.Application application;
    
    /**
     * Create an application from a Scala Application value.
     */
    public Application(play.api.Application application) {
        this.application = application;
    }
    
    /**
     * Retrieve the application path.
     *
     * @return The application path.
     */
    public File path() {
        return application.path();
    }
    
    /**
     * Retrieve the application classloader.
     *
     * @return The application classloader.
     */
    public ClassLoader classloader() {
        return application.classloader();
    }
    
    /**
     * Retrieve a file relatively to the application root path.
     *
     * @param relativePath Relative path of the file to fetch. 
     * @return A file instance, but it is not guaranteed that the file exist.
     */
    public File getFile(String relativePath) {
        return application.getFile(relativePath);
    }
    
    /**
     * Scan the application classloader to retrieve all types annotated with a specific annotation.
     *
     * This method is useful for some plugins, for example the EBean plugin will automatically detect all types
     * annotated with @javax.persistance.Entity.
     *
     * Note that it is better to specify a very specific package to avoid too expensive searchs.
     *
     * @param packageName The root package to scan,
     * @param annotation Annotation class.
     * @return A set of types names statifying the condition.
     */
    public Set<String> getTypesAnnotatedWith(String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return scala.collection.JavaConverters.setAsJavaSetConverter(
            application.getTypesAnnotatedWith(packageName, annotation)
        ).asJava();
    }
    
}