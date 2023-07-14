/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

// #content

import static org.junit.jupiter.api.Assertions.assertTrue;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;

class GitHubClientTest {
  private static GitHubClient client;
  private static WSClient ws;
  private static Server server;

  @BeforeAll
  static void setup() {
    server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/repositories")
                    .routingTo(
                        request -> {
                          ArrayNode repos = Json.newArray();
                          ObjectNode repo = Json.newObject();
                          repo.put("full_name", "octocat/Hello-World");
                          repos.add(repo);
                          return ok(repos);
                        })
                    .build());
    ws = play.test.WSTestClient.newClient(server.httpPort());
    client = new GitHubClient(ws);
    client.baseUrl = "";
  }

  @AfterAll
  static void tearDown() throws IOException {
    try {
      ws.close();
    } finally {
      server.stop();
    }
  }

  @Test
  void repositories() throws Exception {
    List<String> repos = client.getRepositories().toCompletableFuture().get(10, TimeUnit.SECONDS);
    assertTrue(repos.stream().anyMatch(item -> item.equals("octocat/Hello-World")));
  }
}
// #content
