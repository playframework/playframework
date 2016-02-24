/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

//#content
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import play.routing.Router;
import play.libs.Json;
import play.libs.ws.*;
import play.routing.RoutingDsl;
import play.server.Server;

import static play.mvc.Results.*;
import static org.junit.Assert.*;
import static org.hamcrest.core.IsCollectionContaining.*;

public class GitHubClientTest {
    GitHubClient client;
    WSClient ws;
    Server server;

    @Before
    public void setup() {
        Router router = new RoutingDsl()
            .GET("/repositories").routeTo(() -> {
                ArrayNode repos = Json.newArray();
                ObjectNode repo = Json.newObject();
                repo.put("full_name", "octocat/Hello-World");
                repos.add(repo);
                return ok(repos);
            })
            .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        client = new GitHubClient();
        client.baseUrl = "";
        client.ws = ws;
    }

    @After
    public void tearDown() throws IOException {
        try {
            ws.close();
        }
        finally {
            server.stop();
        }
    }

    @Test
    public void repositories() throws Exception {
        List<String> repos = client.getRepositories()
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(repos, hasItem("octocat/Hello-World"));
    }
}
//#content
