/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.test;

import play.core.server.NettyServer;
import scala.Option;

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
        super(port, application.getWrappedApplication(), Option.<Object>apply(null), NettyServer.defaultServerProvider());
    }

    /**
     * A test Netty web server with HTTPS support
     * @param port HTTP port to bind on
     * @param application The FakeApplication to load in this server
     * @param sslPort HTTPS port to bind on
     */
    public TestServer(int port, FakeApplication application, int sslPort) {
        super(port, application.getWrappedApplication(), Option.<Object>apply(sslPort), NettyServer.defaultServerProvider());
    }

}
