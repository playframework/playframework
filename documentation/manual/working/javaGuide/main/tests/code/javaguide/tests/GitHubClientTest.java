package javaguide.tests;

//#content
import java.util.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import play.api.routing.Router;
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
    public void tearDown() {
        ws.close();
        server.stop();
    }

    @Test
    public void repositories() {
        List<String> repos = client.getRepositories().get(10000);
        assertThat(repos, hasItem("octocat/Hello-World"));
    }
}
//#content
