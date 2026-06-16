/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

import org.apache.pekko.actor.*;
import org.apache.pekko.stream.Materializer;
import play.libs.streams.ActorFlow;
import play.mvc.Result;
import play.mvc.WebSocket;
import play.libs.F;

// #streams-imports
import org.apache.pekko.stream.javadsl.*;
// #streams-imports

import play.mvc.Controller;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JavaWebSockets {

  public static class Actor1 extends AbstractActor {
    private final Closeable someResource;

    public Actor1(Closeable someResource) {
      this.someResource = someResource;
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
          // match() messages here...
          .build();
    }

    // #actor-post-stop
    public void postStop() throws Exception {
      someResource.close();
    }
    // #actor-post-stop
  }

  public static class Actor2 extends AbstractActor {
    @Override
    public Receive createReceive() {
      return receiveBuilder()
          // match() messages here
          .build();
    }

    {
      // #actor-stop
      self().tell(PoisonPill.getInstance(), self());
      // #actor-stop
    }
  }

  public static class ActorController2 extends Controller {
    private ActorSystem actorSystem;
    private Materializer materializer;

    // #actor-reject
    public WebSocket socket() {
      return WebSocket.Text.acceptOrResult(
          request ->
              CompletableFuture.completedFuture(
                  request
                      .session()
                      .get("user")
                      .map(
                          user ->
                              F.Either.<Result, Flow<String, String, ?>>Right(
                                  ActorFlow.actorRef(
                                      MyWebSocketActor::props, actorSystem, materializer)))
                      .orElseGet(() -> F.Either.Left(forbidden()))));
    }

    // #actor-reject

    // #accept-asynchronously
    public WebSocket socketAsync() {
      return WebSocket.Text.acceptOrResult(
          request ->
              CompletableFuture.supplyAsync(() -> "Do some async action ...")
                  .thenApply(
                      __ ->
                          F.Either.Right(
                              ActorFlow.actorRef(
                                  MyWebSocketActor::props, actorSystem, materializer))));
    }
    // #accept-asynchronously
  }

  public static class ActorController4 extends Controller {
    private ActorSystem actorSystem;
    private Materializer materializer;

    // #actor-json
    public WebSocket socket() {
      return WebSocket.Json.accept(
          request -> ActorFlow.actorRef(MyWebSocketActor::props, actorSystem, materializer));
    }
    // #actor-json
  }

  public static class InEvent {}

  public static class OutEvent {}

  public static class ActorController5 extends Controller {
    private ActorSystem actorSystem;
    private Materializer materializer;

    // #actor-json-class
    public WebSocket socket() {
      return WebSocket.json(InEvent.class)
          .accept(
              request -> ActorFlow.actorRef(MyWebSocketActor::props, actorSystem, materializer));
    }
    // #actor-json-class
  }

  public static class Controller1 extends Controller {
    // #streams1
    public WebSocket socket() {
      return WebSocket.Text.accept(
          request -> {
            // Log events to the console
            Sink<String, ?> in = Sink.foreach(System.out::println);

            // Send a single 'Hello!' message and then leave the socket open
            Source<String, ?> out = Source.single("Hello!").concat(Source.maybe());

            return Flow.fromSinkAndSource(in, out);
          });
    }
    // #streams1
  }

  public static class Controller2 extends Controller {

    // #streams2
    public WebSocket socket() {
      return WebSocket.Text.accept(
          request -> {
            // Just ignore the input
            Sink<String, ?> in = Sink.ignore();

            // Send a single 'Hello!' message and close
            Source<String, ?> out = Source.single("Hello!");

            return Flow.fromSinkAndSource(in, out);
          });
    }
    // #streams2

  }

  public static class Controller3 extends Controller {

    // #streams3
    public WebSocket socket() {
      return WebSocket.Text.accept(
          request -> {

            // log the message to stdout and send response back to client
            return Flow.<String>create()
                .map(
                    msg -> {
                      System.out.println(msg);
                      return "I received your message: " + msg;
                    });
          });
    }
    // #streams3
  }

  public static class Controller4 extends Controller {
    // #subprotocol
    private final List<Map.Entry<String, Flow<String, String, ?>>> supportedProtocols =
        List.of(
            Map.entry(
                "graphql-transport-ws", Flow.fromSinkAndSource(Sink.ignore(), Source.maybe())),
            Map.entry("graphql-ws", Flow.fromSinkAndSource(Sink.ignore(), Source.maybe())));

    public WebSocket socket() {
      return WebSocket.Text.acceptOrResultWithOptions(
          request -> {
            String header = request.header("Sec-WebSocket-Protocol").orElse("");
            java.util.List<String> offered =
                Arrays.stream(header.split(",")).map(String::trim).toList();

            return supportedProtocols.stream()
                .filter(entry -> offered.contains(entry.getKey()))
                .findFirst()
                .<CompletableFuture<F.Either<Result, WebSocket.Accepted<String, String>>>>map(
                    entry ->
                        CompletableFuture.completedFuture(
                            F.Either.Right(
                                new WebSocket.Accepted<>(entry.getValue(), entry.getKey()))))
                .orElseGet(
                    () ->
                        CompletableFuture.completedFuture(
                            F.Either.Left(badRequest("Unsupported WebSocket subprotocol"))));
          });
    }
    // #subprotocol
  }

  public static class Controller5 extends Controller {
    // #handshake-options
    public WebSocket socket() {
      return WebSocket.Text.acceptWithOptions(
          request -> {
            Flow<String, String, ?> flow =
                Flow.fromSinkAndSource(Sink.ignore(), Source.<String>maybe());
            return new WebSocket.Accepted<>(flow)
                .withHeader("X-WebSocket-Trace", request.id().toString())
                .withCookies(
                    play.mvc.Http.Cookie.builder("ws-session", "connected")
                        .withHttpOnly(true)
                        .build())
                .addingToSession(request, "websocket", "connected");
          });
    }
    // #handshake-options
  }
}
