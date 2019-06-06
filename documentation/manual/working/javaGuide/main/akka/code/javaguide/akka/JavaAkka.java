/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

import akka.actor.*;
import static akka.pattern.PatternsCS.ask;
import com.typesafe.config.*;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Test;

import play.Application;
import play.core.j.JavaHandlerComponents;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;
import scala.concurrent.*;
import scala.concurrent.duration.Duration;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

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
  public void testask() throws Exception {
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
            assertThat(message, equalTo("Hello, world"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void injected() throws Exception {
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
            assertThat(message, equalTo("foo"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void factoryinjected() throws Exception {
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
                    ask(parent, new ParentActorProtocol.GetChild("my.config"), 1000)
                        .thenApply(msg -> (ActorRef) msg)
                        .thenCompose(
                            child -> ask(child, new ConfiguredChildActorProtocol.GetConfig(), 1000))
                        .toCompletableFuture()
                        .get(5, TimeUnit.SECONDS);
            assertThat(message, equalTo("foo"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void conf() throws Exception {
    Config config = ConfigFactory.parseURL(getClass().getResource("akka.conf"));
    scala.concurrent.Future<Terminated> future = ActorSystem.create("conf", config).terminate();
    Await.ready(future, Duration.create("10s"));
  }

  @Test
  public void async() throws Exception {
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
                  app.getWrappedApplication().materializer());
          assertThat(contentAsString(result), equalTo("Got 2"));
        });
  }
}
