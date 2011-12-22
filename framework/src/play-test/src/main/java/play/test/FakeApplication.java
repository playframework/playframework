package play.test;

import java.io.*;

public class FakeApplication {
    
    final play.api.test.FakeApplication wrappedApplication;
    
    public FakeApplication(File path, ClassLoader classloader) {
        wrappedApplication = new play.api.test.FakeApplication(path, classloader);
    }
    
    public play.api.test.FakeApplication getWrappedApplication() {
        return wrappedApplication;
    }
    
}