/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test.junit5;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.test.Helpers;
import play.test.TestBrowser;

class BrowserServerExtensionTest {
  @RegisterExtension
  static BrowserServerExtension browserServer =
      new BrowserServerExtension(
          Helpers.testBrowser(8080), Helpers.testServer(8080, Helpers.fakeApplication()));

  @Test
  void withBrowserShouldProvideABrowser() {
    TestBrowser browser = browserServer.getTestBrowser();

    assertNotNull(browser);
    browser.goTo("/");
    assertNotNull(browser.pageSource());
    assertTrue(browser.pageSource().contains("Action Not Found"));
  }
}
