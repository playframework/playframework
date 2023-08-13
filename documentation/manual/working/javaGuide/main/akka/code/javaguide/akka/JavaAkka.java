/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka;

import static akka.pattern.Patterns.ask;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.actor.*;
import com.typesafe.config.*;
import java.util.concurrent.*;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.jupiter.api.Test;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import scala.concurrent.*;
import scala.concurrent.duration.Duration;

public class JavaAkka {

  private static volatile CountDownLatch latch;

  public static class MyActor extends AbstractActor {
    @Override
    public Receive createReceive() {
      return receiveBuilder()
          .matchAny(
              m -> {
                latch.countDown();
              })
          .build();
    }
  }

  @Test
  void testask() throws Exception {
    Application app = fakeApplication();
    running(
        app,
        () -> {
          javaguide.akka.ask.Application controller =
              app.injector().instanceOf(javaguide.akka.ask.Application.class);

          try {
            String message =
                contentAsString(
                    controller.sayHello("world").toCompletableFuture().get(1, TimeUnit.SECONDS));
            assertEquals("Hello, world", message);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void injected() throws Exception {
    Application app =
        new GuiceApplicationBuilder()
            .bindings(new javaguide.akka.modules.MyModule())
            .configure("my.config", "foo")
            .build();
    running(
        app,
        () -> {
          javaguide.akka.inject.Application controller =
              app.injector().instanceOf(javaguide.akka.inject.Application.class);

          try {
            String message =
                contentAsString(
                    controller.getConfig().toCompletableFuture().get(1, TimeUnit.SECONDS));
            assertEquals("foo", message);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void factoryinjected() throws Exception {
    Application app =
        new GuiceApplicationBuilder()
            .bindings(new javaguide.akka.factorymodules.MyModule())
            .configure("my.config", "foo")
            .build();
    running(
        app,
        () -> {
          ActorRef parent =
              app.injector()
                  .instanceOf(
                      play.inject.Bindings.bind(ActorRef.class).qualifiedWith("parent-actor"));

          try {
            String message =
                (String)
                    ask(
                            parent,
                            new ParentActorProtocol.GetChild("my.config"),
                            java.time.Duration.ofMillis(1000))
                        .thenApply(msg -> (ActorRef) msg)
                        .thenCompose(
                            child ->
                                ask(
                                    child,
                                    new ConfiguredChildActorProtocol.GetConfig(),
                                    java.time.Duration.ofMillis(1000)))
                        .toCompletableFuture()
                        .get(5, TimeUnit.SECONDS);
            assertEquals("foo", message);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void conf() throws Exception {
    Config config = ConfigFactory.parseURL(getClass().getResource("akka.conf"));
    scala.concurrent.Future<Terminated> future = ActorSystem.create("conf", config).terminate();
    Await.ready(future, Duration.create("10s"));
  }

  @Test
  void async() throws Exception {
    Application app = fakeApplication();
    running(
        app,
        () -> {
          Result result =
              MockJavaActionHelper.call(
                  new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                    public CompletionStage<Result> index() {
                      return new javaguide.akka.async.Application().index();
                    }
                  },
                  fakeRequest(),
                  app.asScala().materializer());
          assertEquals("Got 2", contentAsString(result));
        });
  }
}
