package play.test;

import java.io.*;
import java.util.*;

import play.libs.*;

public class FakeApplication {
    
    final play.api.test.FakeApplication wrappedApplication;
    
    public FakeApplication(File path, ClassLoader classloader, Map<String,String> additionalConfiguration) {
        wrappedApplication = new play.api.test.FakeApplication(
            path, 
            classloader, 
            Scala.<String>emptySeq(), 
            Scala.<String>emptySeq(), 
            Scala.asScala(additionalConfiguration)
        );
    }
    
    public play.api.test.FakeApplication getWrappedApplication() {
        return wrappedApplication;
    }
    
}