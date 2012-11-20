package play.test;

import org.junit.After;
import play.api.test.Helpers$;

/**
 * Provides a server to JUnit tests
 */
public class WithServer {

    protected FakeApplication app;
    protected int port;
    protected TestServer testServer;

    protected void start() {
        start(Helpers.fakeApplication());
    }

    protected void start(FakeApplication fakeApplication) {
        start(fakeApplication, Helpers$.MODULE$.testServerPort());
    }

    protected void start(FakeApplication fakeApplication, int port) {
        this.port = port;
        testServer = Helpers.testServer(port, fakeApplication);
        testServer.start();
    }

    @After
    public void stopServer() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
            app = null;
        }
    }
}
