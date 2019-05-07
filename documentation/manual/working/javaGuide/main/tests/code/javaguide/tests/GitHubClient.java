/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

// #client
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;

class GitHubClient {
  private WSClient ws;

  @Inject
  public GitHubClient(WSClient ws) {
    this.ws = ws;
  }

  String baseUrl = "https://api.github.com";

  public CompletionStage<List<String>> getRepositories() {
    return ws.url(baseUrl + "/repositories")
        .get()
        .thenApply(
            response ->
                response.asJson().findValues("full_name").stream()
                    .map(JsonNode::asText)
                    .collect(Collectors.toList()));
  }
}
// #client
