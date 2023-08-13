/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

// #client

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
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
