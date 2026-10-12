/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import play.test.Helpers;

public class WithBrowserTest {

  @Test
  public void usesTheTestBrowser() {
    Helpers.running(
        Helpers.testServer(0),
        Helpers.HTMLUNIT,
        browser -> {
          browser.goTo("/");
          assertEquals("Hello browser", browser.el("#greeting").getText());
        });
  }
}
