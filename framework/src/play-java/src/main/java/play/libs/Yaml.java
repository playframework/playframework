/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import java.io.*;
import java.util.*;

import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.*;

/**
 * Yaml utilities.
 */
public class Yaml {
    
    /**
     * Load a Yaml file from the classpath.
     */
    public static Object load(String resourceName) {
        return load(
            play.Play.application().resourceAsStream(resourceName),
            play.Play.application().classloader()
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
