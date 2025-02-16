/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

import akka.actor.*;
import akka.stream.Materializer;
import play.libs.streams.ActorFlow;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import play.libs.F;

// #streams-imports
import akka.stream.javadsl.*;
// #streams-imports

import play.mvc.Controller;

import java.io.Closeable;
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
}
