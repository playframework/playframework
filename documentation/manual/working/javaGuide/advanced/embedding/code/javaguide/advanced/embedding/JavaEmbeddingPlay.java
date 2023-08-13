/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.embedding;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

// #imports
import play.routing.RoutingDsl;
import play.server.Server;
import static play.mvc.Controller.*;
// #imports

import static org.junit.jupiter.api.Assertions.*;

public class JavaEmbeddingPlay {

  @Test
  void simple() throws Exception {
    // #simple
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/hello/:to")
                    .routingTo((request, to) -> ok("Hello " + to))
                    .build());
    // #simple

    try {
      withClient(
          ws -> {
            // #http-port
            CompletionStage<WSResponse> response =
                ws.url("http://localhost:" + server.httpPort() + "/hello/world").get();
            // #http-port
            try {
              assertEquals("Hello world", response.toCompletableFuture().get(10, TimeUnit.SECONDS).getBody());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      // #stop
      server.stop();
      // #stop
    }
  }

  @Test
  void config() throws Exception {
    // #config
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/hello/:to")
                    .routingTo((request, to) -> ok("Hello " + to))
                    .build());
    // #config

    try {
      withClient(
          ws -> {
            try {
              assertEquals(
                      "Hello world",
                  ws.url("http://localhost:" + server.httpPort() + "/hello/world")
                      .get()
                      .toCompletableFuture()
                      .get(10, TimeUnit.SECONDS)
                      .getBody());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      server.stop();
    }
  }

  private void withClient(Consumer<WSClient> callback) throws IOException {
    try (WSClient client = play.test.WSTestClient.newClient(19000)) {
      callback.accept(client);
    }
  }
}
