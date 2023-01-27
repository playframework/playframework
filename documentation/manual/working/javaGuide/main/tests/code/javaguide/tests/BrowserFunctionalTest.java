/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import static org.junit.Assert.*;

import org.junit.*;
import play.test.*;

// #test-withbrowser
public class BrowserFunctionalTest extends WithBrowser {

  @Test
  public void runInBrowser() {
    browser.goTo("/");
    assertNotNull(browser.el("title").text());
  }
}
// #test-withbrowser
