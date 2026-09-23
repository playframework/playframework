/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import play.http.ActionCreator;
import play.libs.F;
import play.libs.typedmap.TypedKey;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Security;
import play.mvc.WebSocket;
import scala.concurrent.Promise;

/** Java actions for WebSocket spec */
public class WebSocketSpecJavaActions {
  public static class JavaJsonMessage {
    public int count;
  }

  private static final TypedKey<String> ACTION_CREATOR_ATTRIBUTE =
      TypedKey.create("websocket-action-creator");

  public static class AllowAuthenticator extends Security.Authenticator {
    @Override
    public Optional<String> getUsername(Http.Request request) {
      return Optional.of("controller");
    }
  }

  public static class RejectAuthenticator extends Security.Authenticator {
    @Override
    public Optional<String> getUsername(Http.Request request) {
      return Optional.empty();
    }
  }

  public static class WebSocketActionCreator implements ActionCreator {
    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
      return new Action.Simple() {
        @Override
        public CompletionStage<Result> call(Http.Request request) {
          return delegate.call(request.addAttr(ACTION_CREATOR_ATTRIBUTE, "creator"));
        }
      };
    }
  }

  @Security.Authenticated(AllowAuthenticator.class)
  public static class ActionCompositionController {
    private final AtomicBoolean rejectedInvoked = new AtomicBoolean();

    public WebSocket accepted() {
      return WebSocket.Text.accept(
          request ->
              Flow.fromSinkAndSource(
                  Sink.ignore(),
                  Source.single(
                      request.attrs().getOptional(Security.USERNAME).orElse("missing")
                          + ":"
                          + request
                              .attrs()
                              .getOptional(ACTION_CREATOR_ATTRIBUTE)
                              .orElse("missing"))));
    }

    @Security.Authenticated(RejectAuthenticator.class)
    public WebSocket rejected() {
      rejectedInvoked.set(true);
      return accepted();
    }

    public boolean wasRejectedInvoked() {
      return rejectedInvoked.get();
    }
  }

  private static <A> Sink<A, ?> getChunks(Consumer<List<A>> onDone) {
    return Sink.<List<A>, A>fold(
            new ArrayList<>(),
            (result, next) -> {
              result.add(next);
              return result;
            })
        .mapMaterializedValue(future -> future.thenAccept(onDone));
  }

  private static <A> Source<A, ?> emptySource() {
    return Source.completionStage(new CompletableFuture<>());
  }

  public static WebSocket allowConsumingMessages(Promise<List<String>> messages) {
    return WebSocket.Text.accept(
        request -> Flow.fromSinkAndSource(getChunks(messages::success), emptySource()));
  }

  public static WebSocket allowSendingMessages(List<String> messages) {
    return WebSocket.Text.accept(
        request -> Flow.fromSinkAndSource(Sink.ignore(), Source.from(messages)));
  }

  public static WebSocket closeWhenTheConsumerIsDone() {
    return WebSocket.Text.accept(
        request -> Flow.fromSinkAndSource(Sink.cancelled(), emptySource()));
  }

  public static WebSocket allowRejectingAWebSocketWithAResult(int statusCode) {
    return WebSocket.Text.acceptOrResult(
        request -> CompletableFuture.completedFuture(F.Either.Left(Results.status(statusCode))));
  }

  public static WebSocket selectSubprotocol() {
    return WebSocket.Text.acceptWithOptions(
        request ->
            new WebSocket.Accepted<>(
                Flow.fromSinkAndSource(Sink.ignore(), Source.empty()), "graphql-transport-ws"));
  }

  public static WebSocket selectSubprotocolWithoutCompression() {
    return WebSocket.Text.acceptWithOptions(
        request ->
            new WebSocket.Accepted<>(
                Flow.fromSinkAndSource(Sink.ignore(), Source.single("plain server message")),
                "graphql-transport-ws",
                false));
  }

  public static WebSocket selectMessagesForCompression() {
    return WebSocket.Text.acceptWithOptions(
        request ->
            new WebSocket.Accepted<>(
                Flow.fromSinkAndSource(Sink.ignore(), Source.from(List.of("\u20ac", "123456"))),
                context ->
                    context.message() instanceof play.http.websocket.Message.Text
                        && context.payloadLength() == 3
                        && !context.isAboveCompressionThreshold()));
  }

  public static WebSocket acceptText() {
    return WebSocket.Text.accept(request -> Flow.fromSinkAndSource(Sink.ignore(), emptySource()));
  }

  public static WebSocket acceptBinary() {
    return WebSocket.Binary.accept(request -> Flow.fromSinkAndSource(Sink.ignore(), emptySource()));
  }

  public static WebSocket acceptJson() {
    return WebSocket.Json.accept(request -> Flow.fromSinkAndSource(Sink.ignore(), emptySource()));
  }

  public static WebSocket acceptJsonClass() {
    return WebSocket.json(JavaJsonMessage.class)
        .accept(request -> Flow.fromSinkAndSource(Sink.ignore(), emptySource()));
  }

  public static WebSocket addHandshakeHeadersAndCookies() {
    return WebSocket.Text.acceptWithOptions(
        request -> {
          Flow<String, String, ?> flow =
              Flow.fromSinkAndSource(Sink.ignore(), Source.single("plain server message"));
          return new WebSocket.Accepted<>(flow, "graphql-transport-ws", false)
              .withHeaders(
                  "X-WebSocket-Trace",
                  "discarded",
                  "x-websocket-trace",
                  "java-trace",
                  "X-Remove",
                  "remove",
                  "Set-Cookie",
                  "java-raw-cookie=raw-value; Path=/")
              .withoutHeader("x-remove")
              .withCookies(Http.Cookie.builder("java-ws-cookie", "cookie-value").build())
              .discardingCookie("java-expired", "/", null, true, Http.Cookie.SameSite.LAX, true)
              .addingToSession(request, "websocket", "connected");
        });
  }
}
