package play.libs;

import java.io.*;
import java.util.*;

import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.*;

public class Yaml {
    
    public static Object load(String resourceName) {
        return load(
            play.Play.application().resourceAsStream(resourceName),
            play.Play.application().classloader()
        );
    }
    
    public static Object load(InputStream is, ClassLoader classloader) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new CustomClassLoaderConstructor(classloader));
        return yaml.load(is);
    }
    
}