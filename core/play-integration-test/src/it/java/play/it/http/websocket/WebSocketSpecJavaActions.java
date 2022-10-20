/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import play.libs.F;
import play.mvc.Results;
import play.mvc.WebSocket;
import scala.concurrent.Promise;
import scala.jdk.javaapi.FutureConverters;

/** Java actions for WebSocket spec */
public class WebSocketSpecJavaActions {

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
    return Source.fromFuture(FutureConverters.asScala(new CompletableFuture<>()));
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
}
