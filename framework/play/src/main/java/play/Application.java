package play;

import java.io.*;
import java.util.*;

public class Application {
    
    final play.api.Application application;
    
    public Application(play.api.Application application) {
        this.application = application;
    }
    
    public File path() {
        return application.path();
    }
    
    public ClassLoader classloader() {
        return application.classloader();
    }
    
    public File getFile(String relativePath) {
        return application.getFile(relativePath);
    }
    
    public Set<String> getTypesAnnotatedWith(String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return scala.collection.JavaConverters.setAsJavaSetConverter(
            application.getTypesAnnotatedWith(packageName, annotation)
        ).asJava();
    }
    
}