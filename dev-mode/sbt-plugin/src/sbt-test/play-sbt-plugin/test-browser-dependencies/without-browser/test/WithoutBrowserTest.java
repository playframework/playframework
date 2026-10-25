/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import play.test.Helpers;
import play.test.TestServer;

public class WithoutBrowserTest {

  @Test
  public void browserConstantsAreNull() {
    assertNull(Helpers.HTMLUNIT);
    assertNull(Helpers.FIREFOX);
  }

  @Test
  public void runningAServerWorks() {
    TestServer server = Helpers.testServer(0);
    Helpers.running(server, () -> assertTrue(server.getRunningHttpPort().getAsInt() > 0));
  }

  @Test
  public void testBrowserExplainsTheMissingDependencies() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Helpers.testBrowser());
    assertTrue(e.getMessage(), e.getMessage().contains("playTestBrowser"));
  }
}
