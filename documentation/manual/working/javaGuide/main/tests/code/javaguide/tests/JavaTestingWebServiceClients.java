/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Controller.*;

import com.fasterxml.jackson.databind.node.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;

public class JavaTestingWebServiceClients {

  @Test
  public void mockService() {
    // #mock-service
    Server server =
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
    // #mock-service

    server.stop();
  }

  @Test
  public void sendResource() throws Exception {
    // #send-resource
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/repositories")
                    .routingTo(request -> ok().sendResource("github/repositories.json"))
                    .build());
    // #send-resource

    WSClient ws = play.test.WSTestClient.newClient(server.httpPort());
    GitHubClient client = new GitHubClient(ws);
    client.baseUrl = "";

    try {
      List<String> repos = client.getRepositories().toCompletableFuture().get(10, TimeUnit.SECONDS);
      assertThat(repos).contains("octocat/Hello-World");
    } finally {
      try {
        ws.close();
      } finally {
        server.stop();
      }
    }
  }
}
