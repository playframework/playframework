/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import org.junit.After;
import org.junit.Before;

/**
 * Provides a server to JUnit tests. Make your test class extend this class and an HTTP server will be started before each test is invoked.
 * You can setup the fake application and port to use by overriding the provideFakeApplication and providePort methods.
 * Within a test, the running application and the TCP port are available through the app and port fields, respectively.
 */
public class WithServer {

    protected FakeApplication app;
    protected int port;
    protected TestServer testServer;

    /**
     * Override this method to setup the fake application to use.
     *
     * @return The fake application used by the server
     */
    protected FakeApplication provideFakeApplication() {
        return Helpers.fakeApplication();
    }

    /**
     * Override this method to setup the port to use.
     *
     * @return The TCP port used by the server
     */
    protected int providePort() {
        return play.api.test.Helpers.testServerPort();
    }

    /**
     * @deprecated The server is automatically started before each test, you don’t need to call this method explicitly
     */
    @Deprecated
    protected void start() {
        start(provideFakeApplication());
    }

    /**
     * @deprecated The server is automatically started before each test. You can setup the fake application to use by overriding the provideFakeApplication method.
     */
    @Deprecated
    protected void start(FakeApplication fakeApplication) {
        start(fakeApplication, providePort());
    }

    /**
     * @deprecated The server is automatically started before each test. You can setup the fake application and port to use by overriding the provideFakeApplication and providePort methods, respectively.
     */
    @Deprecated
    protected void start(FakeApplication fakeApplication, int port) {
        // prevents multiple starts
        if (testServer != null) {
            testServer.stop();
        }
        this.port = port;
        this.app = fakeApplication;
        testServer = Helpers.testServer(port, fakeApplication);
        testServer.start();
    }

    @Before
    public void startServer() {
        start(provideFakeApplication(), providePort());
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
