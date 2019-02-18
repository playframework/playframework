/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

// #test-withbrowser
public class BrowserFunctionalTest extends WithBrowser {

  @Test
  public void runInBrowser() {
    browser.goTo("/");
    assertNotNull(browser.$("title").text());
  }
}
// #test-withbrowser
