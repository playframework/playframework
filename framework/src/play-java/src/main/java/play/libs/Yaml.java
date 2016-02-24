/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.io.*;
import org.yaml.snakeyaml.constructor.*;
import play.Application;
import play.api.Play;

/**
 * Yaml utilities.
 */
public class Yaml {
    
    /**
     * Load a Yaml file from the classpath.
     */
    public static Object load(String resourceName) {
        final Application application = Play.current().injector().instanceOf(Application.class);

        return load(
            application.resourceAsStream(resourceName),
            application.classloader()
        );
    }
    
    /** 
     * Load the specified InputStream as Yaml.
     *
     * @param classloader The classloader to use to instantiate Java objects.
     */
    public static Object load(InputStream is, ClassLoader classloader) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new CustomClassLoaderConstructor(classloader));
        return yaml.load(is);
    }
    
}
