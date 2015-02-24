package javaguide.tests;

//#client
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;
import play.libs.F;

class GitHubClient {
    @Inject WSClient ws;
    String baseUrl = "https://api.github.com";

    public F.Promise<List<String>> getRepositories() {
        return ws.url(baseUrl + "/repositories").get().map(response ->
            response.asJson().findValues("full_name").stream()
                .map(JsonNode::asText).collect(Collectors.toList())
        );
    }
}
//#client
