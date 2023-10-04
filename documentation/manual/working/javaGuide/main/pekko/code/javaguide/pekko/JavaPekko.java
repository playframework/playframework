/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko;

import static org.apache.pekko.pattern.Patterns.ask;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

import com.typesafe.config.*;
import java.util.concurrent.*;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.apache.pekko.actor.*;
import org.junit.Test;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import scala.concurrent.*;
import scala.concurrent.duration.Duration;

public class JavaPekko {

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
          javaguide.pekko.ask.Application controller =
              app.injector().instanceOf(javaguide.pekko.ask.Application.class);

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
            .bindings(new javaguide.pekko.modules.MyModule())
            .configure("my.config", "foo")
            .build();
    running(
        app,
        () -> {
          javaguide.pekko.inject.Application controller =
              app.injector().instanceOf(javaguide.pekko.inject.Application.class);

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
            .bindings(new javaguide.pekko.factorymodules.MyModule())
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
            assertThat(message, equalTo("foo"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void conf() throws Exception {
    Config config = ConfigFactory.parseURL(getClass().getResource("pekko.conf"));
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
                      return new javaguide.pekko.async.Application().index();
                    }
                  },
                  fakeRequest(),
                  app.asScala().materializer());
          assertThat(contentAsString(result), equalTo("Got 2"));
        });
  }
}
