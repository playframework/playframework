/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import org.junit.Test;
import scala.util.Properties;

public class TestServerTest {
  @Test
  public void shouldReturnHttpPort() {
    int testServerPort = play.api.test.Helpers.testServerPort();
    final TestServer testServer = Helpers.testServer(testServerPort);
    testServer.start();
    assertTrue("No value for http port", testServer.getRunningHttpPort().isPresent());
    assertFalse(
        "https port value is present, but was not set",
        testServer.getRunningHttpsPort().isPresent());
    assertTrue(
        "The os provided http port is not greater than 0",
        testServer.getRunningHttpPort().getAsInt() > 0);
    testServer.stop();
  }

  @Test
  public void shouldReturnHttpAndHttpsPorts() {
    assumeFalse(Properties.isJavaAtLeast(21)); // because of lightbend/ssl-config#367
    int port = play.api.test.Helpers.testServerPort();
    int httpsPort = 0;
    final TestServer testServer = Helpers.testServer(port, httpsPort);
    testServer.start();
    assertTrue("No value for https port", testServer.getRunningHttpsPort().isPresent());
    assertTrue(
        "The os provided https port is not greater than 0",
        testServer.getRunningHttpsPort().getAsInt() > 0);
    assertTrue("No value for http port", testServer.getRunningHttpPort().isPresent());
    assertTrue(
        "The os provided http port is not greater than 0",
        testServer.getRunningHttpPort().getAsInt() > 0);
    testServer.stop();
  }
}
