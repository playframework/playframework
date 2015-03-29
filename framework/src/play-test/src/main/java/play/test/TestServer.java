/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import play.Application;
import play.core.server.ServerProvider;
import scala.Option;

/**
 * A test Netty web server.
 */
public class TestServer extends play.api.test.TestServer {

    /**
     * A test Netty web server.
     *
     * @param port HTTP port to bind on.
     * @param application The Application to load in this server.
     */
    public TestServer(int port, Application application) {
        super(port, application.getWrappedApplication(), play.libs.Scala.<Object>None(), play.libs.Scala.<ServerProvider>None());
    }

    /**
     * A test Netty web server with HTTPS support
     * @param port HTTP port to bind on
     * @param application The Application to load in this server
     * @param sslPort HTTPS port to bind on
     */
    public TestServer(int port, Application application, int sslPort) {
        super(port, application.getWrappedApplication(), Option.<Object>apply(sslPort), play.libs.Scala.<ServerProvider>None());
    }

}
