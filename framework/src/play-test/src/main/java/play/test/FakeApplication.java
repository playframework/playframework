package play.test;

import java.io.*;

import play.libs.*;

public class FakeApplication {
    
    final play.api.test.FakeApplication wrappedApplication;
    
    public FakeApplication(File path, ClassLoader classloader) {
        wrappedApplication = new play.api.test.FakeApplication(
            path, 
            classloader, 
            Scala.<String>emptySeq(), 
            Scala.<String>emptySeq(), 
            Scala.<String,String>emptyMap()
        );
    }
    
    public play.api.test.FakeApplication getWrappedApplication() {
        return wrappedApplication;
    }
    
}