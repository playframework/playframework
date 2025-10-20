/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.HttpVerbs.GET;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;

public class ApplicationTest extends AbstractRoutesTest {

  @Test
  public void checkDefaultController() {
    var result = route(app, fakeRequest(GET, "/default/unknown"));
    assertThat(result.status()).isEqualTo(NOT_FOUND);
    result = route(app, fakeRequest(GET, "/default/error"));
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void checkAsyncResult() {
    var result = route(app, fakeRequest(GET, "/result/async?x=10"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("Answer: 20");
  }

  @Test
  public void checkSameEscapedJavaIdentifier() {
    var result = route(app, fakeRequest(GET, "/same-escaped-java-identifier?%5B%5D=a&%25%25=b"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("ab");
  }

  @Test
  public void checkWebSocket() {
    var server = testServer();
    running(
        server,
        () -> {
          try {
            var client =
                new WebSocketTestClient(
                    new URI(
                        String.format(
                            "ws://localhost:%d/result/ws?x=Play",
                            server.getRunningHttpPort().orElse(0))));
            client.connect();
            client.closeBlocking();
            assertThat(client.getReceivedMessages()).containsExactly("Hello, Play");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public static class WebSocketTestClient extends WebSocketClient {

    private final List<String> receivedMessages = new ArrayList<>();

    public WebSocketTestClient(URI serverUri) {
      super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {}

    @Override
    public void onMessage(String message) {
      receivedMessages.add(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {}

    @Override
    public void onError(Exception ex) {}

    public List<String> getReceivedMessages() {
      return receivedMessages;
    }
  }
}
