/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import java.io.File;
import java.util.Optional;
import java.util.OptionalInt;
import play.Application;
import play.Mode;
import play.core.server.ServerConfig;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

/** A test web server. */
public class TestServer extends play.api.test.TestServer {

  /**
   * A test web server.
   *
   * @param port HTTP port to bind on.
   * @param application The Application to load in this server.
   */
  public TestServer(int port, Application application) {
    super(
        createServerConfig(Optional.of(port), Optional.empty()),
        application.asScala(),
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
    super(
        createServerConfig(Optional.of(port), Optional.of(sslPort)),
        application.asScala(),
        play.libs.Scala.None());
  }

  @SuppressWarnings("unchecked")
  private static ServerConfig createServerConfig(
      Optional<Integer> port, Optional<Integer> sslPort) {
    return ServerConfig.apply(
        TestServer.class.getClassLoader(),
        new File("."),
        (Option) OptionConverters.toScala(port),
        (Option) OptionConverters.toScala(sslPort),
        play.api.test.Helpers.testServerAddress(),
        Mode.TEST.asScala(),
        System.getProperties());
  }

  /** The HTTP port that the server is running on. */
  @SuppressWarnings("unchecked")
  public OptionalInt getRunningHttpPort() {
    Option scalaPortOption = runningHttpPort();
    return OptionConverters.toJavaOptionalInt(scalaPortOption);
  }

  /** The HTTPS port that the server is running on. */
  @SuppressWarnings("unchecked")
  public OptionalInt getRunningHttpsPort() {
    Option scalaPortOption = runningHttpsPort();
    return OptionConverters.toJavaOptionalInt(scalaPortOption);
  }
}
