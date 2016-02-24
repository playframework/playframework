/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

import com.fasterxml.jackson.databind.node.*;
import org.junit.Test;
import play.routing.Router;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsCollectionContaining.*;
import static org.junit.Assert.*;
import static play.mvc.Controller.*;

public class JavaTestingWebServiceClients {

    @Test
    public void mockService() {
        //#mock-service
        Router router = new RoutingDsl()
            .GET("/repositories").routeTo(() -> {
                ArrayNode repos = Json.newArray();
                ObjectNode repo = Json.newObject();
                repo.put("full_name", "octocat/Hello-World");
                repos.add(repo);
                return ok(repos);
            })
            .build();

        Server server = Server.forRouter(router);
        //#mock-service

        server.stop();
    }

    @Test
    public void sendResource() throws Exception {
        //#send-resource
        Router router = new RoutingDsl()
            .GET("/repositories").routeTo(() ->
                    ok().sendResource("github/repositories.json")
            )
            .build();
        //#send-resource

        Server server = Server.forRouter(router);

        WSClient ws = WS.newClient(server.httpPort());
        GitHubClient client = new GitHubClient();
        client.baseUrl = "";
        client.ws = ws;

        try {
            List<String> repos = client.getRepositories().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertThat(repos, hasItem("octocat/Hello-World"));
        } finally {
            try {
                ws.close();
            }
            finally {
                server.stop();
            }
        }
    }

}
