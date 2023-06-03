/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import org.junit.After;
import org.junit.Before;
import play.Application;

/**
 * Provides a server to JUnit tests. Make your test class extend this class and an HTTP server will
 * be started before each test is invoked. You can setup the application and port to use by
 * overriding the provideApplication and providePort methods. Within a test, the running application
 * and the TCP port are available through the app and port fields, respectively.
 */
public class WithServer {

  protected Application app;
  protected int port = -1; // avoid 0, it could mean random port assigned by the OS.
  protected TestServer testServer;

  /**
   * Override this method to setup the application to use.
   *
   * @return The application used by the server
   */
  protected Application provideApplication() {
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

  @Before
  public void startServer() {
    if (testServer != null) {
      testServer.stop();
    }
    app = provideApplication();
    testServer = Helpers.testServer(providePort(), app);
    testServer.start();
    port = testServer.getRunningHttpPort().getAsInt();
  }

  @After
  public void stopServer() {
    if (testServer != null) {
      testServer.stop();
      testServer = null;
      port = -1;
      app = null;
    }
  }
}
