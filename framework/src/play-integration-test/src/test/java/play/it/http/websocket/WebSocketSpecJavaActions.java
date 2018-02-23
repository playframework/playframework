/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Results;
import play.mvc.WebSocket;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Java actions for WebSocket spec
 */
public class WebSocketSpecJavaActions {

    private static <A> Sink<A, ?> getChunks(Consumer<List<A>> onDone) {
        return Sink.<List<A>, A>fold(new ArrayList<A>(), (result, next) -> {
            result.add(next);
            return result;
        }).mapMaterializedValue(future -> future.thenAccept(onDone));
    }

    private static <A> Source<A, ?> emptySource() {
        return Source.fromFuture(FutureConverters.toScala(new CompletableFuture<>()));
    }

    public static WebSocket allowConsumingMessages(Promise<List<String>> messages) {
        ensureContext();
        return WebSocket.Text.accept(request -> Flow.fromSinkAndSource(getChunks(messages::success), emptySource()));
    }

    public static WebSocket allowSendingMessages(List<String> messages) {
        ensureContext();
        return WebSocket.Text.accept(request -> Flow.fromSinkAndSource(Sink.ignore(), Source.from(messages)));
    }

    public static WebSocket closeWhenTheConsumerIsDone() {
        ensureContext();
        return WebSocket.Text.accept(request -> Flow.fromSinkAndSource(Sink.cancelled(), emptySource()));
    }

    public static WebSocket allowRejectingAWebSocketWithAResult(int statusCode) {
        ensureContext();
        return WebSocket.Text.acceptOrResult(request -> CompletableFuture.completedFuture(F.Either.Left(Results.status(statusCode))));
    }

    private static Http.Context ensureContext() {
        return Http.Context.current();
    }
}
