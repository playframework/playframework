/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
import java.util.OptionalInt;

/**
 * A test web server.
 */
public class TestServer extends play.api.test.TestServer {

    /**
     * A test web server.
     *
     * @param port HTTP port to bind on.
     * @param application The Application to load in this server.
     */
    public TestServer(int port, Application application) {
        super(createServerConfig(Optional.of(port), Optional.empty()), application.asScala(),
                play.libs.Scala.None());
    }

    /**
     * A test web server with HTTPS support
     *
     * @param port HTTP port to bind on
     * @param application The Application to load in this server
     * @param sslPort HTTPS port to bind on
     */
    public TestServer(int port, Application application, int sslPort) {
        super(createServerConfig(Optional.of(port), Optional.of(sslPort)), application.asScala(),
                play.libs.Scala.None());
    }

    @SuppressWarnings("unchecked")
    private static ServerConfig createServerConfig(Optional<Integer> port, Optional<Integer> sslPort) {
        return ServerConfig.apply(TestServer.class.getClassLoader(), new File("."),
                (Option) OptionConverters.toScala(port), (Option) OptionConverters.toScala(sslPort), "0.0.0.0",
                Mode.TEST.asScala(), System.getProperties());
    }

    /**
     * The HTTP port that the server is running on.
     */
    @SuppressWarnings("unchecked")
    public OptionalInt getRunningHttpPort() {
        Option scalaPortOption = runningHttpPort();
        return OptionConverters.specializer_OptionalInt().fromScala(scalaPortOption);
    }

    /**
     * The HTTPS port that the server is running on.
     */
    @SuppressWarnings("unchecked")
    public OptionalInt getRunningHttpsPort() {
        Option scalaPortOption = runningHttpsPort();
        return OptionConverters.specializer_OptionalInt().fromScala(scalaPortOption);
    }
}
