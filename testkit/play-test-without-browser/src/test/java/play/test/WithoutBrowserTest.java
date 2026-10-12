/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

/**
 * play-test only declares the test browser's dependencies (Selenide, Selenium, HtmlUnit) as
 * optional, apart from selenium-api. This project excludes them, like an application without
 * play-test-browser.
 */
public class WithoutBrowserTest {

  @Test
  public void browsersAreNotOnTheClasspath() {
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("org.openqa.selenium.htmlunit.HtmlUnitDriver"));
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("org.openqa.selenium.firefox.FirefoxDriver"));
    assertThrows(
        ClassNotFoundException.class, () -> Class.forName("com.codeborne.selenide.SelenideDriver"));
  }

  @Test
  public void helpersWork() {
    Result result = Results.ok("hello");
    assertEquals(Helpers.OK, result.status());
    assertEquals("hello", Helpers.contentAsString(result));
    Http.Request request = Helpers.fakeRequest(Helpers.POST, "/").build();
    assertEquals("POST", request.method());
  }

  @Test
  public void runningAnApplicationWorks() {
    Application app = Helpers.fakeApplication();
    Helpers.running(app, () -> assertTrue(app.isTest()));
  }

  @Test
  public void runningAServerWorks() {
    TestServer server = Helpers.testServer();
    Helpers.running(server, () -> assertTrue(server.getRunningHttpPort().getAsInt() > 0));
  }

  @Test
  public void testBrowserExplainsTheMissingDependencies() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Helpers.testBrowser());
    assertTrue(e.getMessage(), e.getMessage().contains("playTestBrowser"));
  }
}
