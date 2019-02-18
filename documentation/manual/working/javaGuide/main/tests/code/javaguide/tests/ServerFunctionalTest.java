/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import play.libs.ws.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

import static play.test.Helpers.NOT_FOUND;

// #test-withserver
public class ServerFunctionalTest extends WithServer {

  @Test
  public void testInServer() throws Exception {
    OptionalInt optHttpsPort = testServer.getRunningHttpsPort();
    String url;
    int port;
    if (optHttpsPort.isPresent()) {
      port = optHttpsPort.getAsInt();
      url = "https://localhost:" + port;
    } else {
      port = testServer.getRunningHttpPort().getAsInt();
      url = "http://localhost:" + port;
    }
    try (WSClient ws = play.test.WSTestClient.newClient(port)) {
      CompletionStage<WSResponse> stage = ws.url(url).get();
      WSResponse response = stage.toCompletableFuture().get();
      assertEquals(NOT_FOUND, response.getStatus());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
// #test-withserver
