package play.test;

import java.io.*;

public class TestServer extends play.api.test.TestServer {
    
    public TestServer(int port, FakeApplication application) {
        super(port, application.getWrappedApplication());
    }
    
}