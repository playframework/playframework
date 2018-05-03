/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import play.Application;
import play.Mode;
import play.core.server.ServerConfig;
import play.core.server.ServerProvider;
import scala.Option;
import scala.compat.java8.OptionConverters;

import java.io.File;
import java.util.Optional;

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
        super(createServerConfig(Optional.of(port), Optional.empty()), application.getWrappedApplication(),
                play.libs.Scala.<ServerProvider>None());
    }

    /**
     * A test Netty web server with HTTPS support
     * @param port HTTP port to bind on
     * @param application The Application to load in this server
     * @param sslPort HTTPS port to bind on
     */
    public TestServer(int port, Application application, int sslPort) {
        super(createServerConfig(Optional.of(port), Optional.of(sslPort)), application.getWrappedApplication(),
                play.libs.Scala.<ServerProvider>None());
    }

    private static ServerConfig createServerConfig(Optional<Integer> port, Optional<Integer> sslPort) {
        return ServerConfig.apply(TestServer.class.getClassLoader(), new File("."),
                (Option) OptionConverters.toScala(port), (Option) OptionConverters.toScala(sslPort), "0.0.0.0",
                Mode.TEST.asScala(), System.getProperties());
    }

}
