/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import org.junit.*;

import play.api.test.WsTestClient;
import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

// #bad-route-import
import play.mvc.Http.RequestBuilder;
// #bad-route-import

// #test-withapp
public class FunctionalTest extends WithApplication {
  // #test-withapp

  // #bad-route
  @Test
  public void testBadRoute() {
    RequestBuilder request = Helpers.fakeRequest().method(GET).uri("/xx/Kiwi");

    Result result = route(app, request);
    assertEquals(NOT_FOUND, result.status());
  }
  // #bad-route

  int timeout = 5000;

  private TestServer testServer() {
    Map<String, String> config = new HashMap<String, String>();
    config.put("play.http.router", "javaguide.tests.Routes");
    return Helpers.testServer(fakeApplication(config));
  }

  private TestServer testServer(int port) {
    Map<String, String> config = new HashMap<String, String>();
    config.put("play.http.router", "javaguide.tests.Routes");
    return Helpers.testServer(port, fakeApplication(config));
  }

  private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("application");

  // #test-server
  @Test
  public void testInServer() throws Exception {
    TestServer server = testServer(3333);
    running(
        server,
        () -> {
          try (WSClient ws = WSTestClient.newClient(3333)) {
            CompletionStage<WSResponse> completionStage = ws.url("/").get();
            WSResponse response = completionStage.toCompletableFuture().get();
            assertEquals(OK, response.getStatus());
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        });
  }
  // #test-server

  // #test-browser
  @Test
  public void runInBrowser() {
    running(
        testServer(),
        HTMLUNIT,
        browser -> {
          browser.goTo("/");
          assertEquals("Welcome to Play!", browser.$("#title").text());
          browser.$("a").click();
          assertEquals("login", browser.url());
        });
  }
  // #test-browser
}
