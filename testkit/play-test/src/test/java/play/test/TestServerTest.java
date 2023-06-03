/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestServerTest {
  @Test
  public void shouldReturnHttpPort() {
    int testServerPort = play.api.test.Helpers.testServerPort();
    final TestServer testServer = Helpers.testServer(testServerPort);
    testServer.start();
    assertTrue(testServer.getRunningHttpPort().isPresent(), "No value for http port");
    assertFalse(
        testServer.getRunningHttpsPort().isPresent(),
        "https port value is present, but was not set");
    assertTrue(
        testServer.getRunningHttpPort().getAsInt() > 0,
        "The os provided http port is not greater than 0");
    testServer.stop();
  }

  @Test
  public void shouldReturnHttpAndHttpsPorts() {
    int port = play.api.test.Helpers.testServerPort();
    int httpsPort = 0;
    final TestServer testServer = Helpers.testServer(port, httpsPort);
    testServer.start();
    assertTrue(testServer.getRunningHttpsPort().isPresent(), "No value for https port");
    assertTrue(
        testServer.getRunningHttpsPort().getAsInt() > 0,
        "The os provided https port is not greater than 0");
    assertTrue(testServer.getRunningHttpPort().isPresent(), "No value for http port");
    assertTrue(
        testServer.getRunningHttpPort().getAsInt() > 0,
        "The os provided http port is not greater than 0");
    testServer.stop();
  }
}
