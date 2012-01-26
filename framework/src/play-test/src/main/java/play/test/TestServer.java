package play.test;

import java.io.*;

/**
 * A test Netty web server.
 */
public class TestServer extends play.api.test.TestServer {
    
    /**
     * A test Netty web server.
     *
     * @param port HTTP port to bind on.
     * @param application The FakeApplication to load in this server.
     */
    public TestServer(int port, FakeApplication application) {
        super(port, application.getWrappedApplication());
    }
    
}