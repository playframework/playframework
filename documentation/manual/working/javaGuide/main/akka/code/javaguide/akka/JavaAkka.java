/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;
import scala.concurrent.duration.Duration;

import javax.inject.Provider;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaAkka {

    private static volatile CountDownLatch latch;

    public static class MyActor extends UntypedActor {
        @Override
        public void onReceive(Object msg) throws Exception {
            latch.countDown();
        }
    }

    @Test
    public void testask() throws Exception {
        Application app = fakeApplication();
        running(app, () -> {
            javaguide.akka.ask.Application controller = app.injector().instanceOf(javaguide.akka.ask.Application.class);

            try {
                String message = contentAsString(controller.sayHello("world").toCompletableFuture().get(1, TimeUnit.SECONDS));
                assertThat(message, equalTo("Hello, world"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void injected() throws Exception {
        Application app = new GuiceApplicationBuilder()
                .bindings(new javaguide.akka.modules.MyModule())
                .configure("my.config", "foo")
                .build();
        running(app, () -> {
            javaguide.akka.inject.Application controller = app.injector().instanceOf(javaguide.akka.inject.Application.class);

            try {
                String message = contentAsString(controller.getConfig().toCompletableFuture().get(1, TimeUnit.SECONDS));
                assertThat(message, equalTo("foo"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void factoryinjected() throws Exception {
        Application app = new GuiceApplicationBuilder()
                .bindings(new javaguide.akka.factorymodules.MyModule())
                .configure("my.config", "foo")
                .build();
        running(app, () -> {
            ActorRef parent = app.injector().instanceOf(play.inject.Bindings.bind(ActorRef.class).qualifiedWith("parent-actor"));

            try {
                String message = (String) FutureConverters.toJava(ask(parent, new ParentActorProtocol.GetChild("my.config"), 1000)).thenCompose(child ->
                        FutureConverters.toJava(ask((ActorRef) child, new ConfiguredChildActorProtocol.GetConfig(), 1000))
                ).toCompletableFuture().get(5, TimeUnit.SECONDS);
                assertThat(message, equalTo("foo"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void conf() throws Exception {
        Config config = ConfigFactory.parseURL(getClass().getResource("akka.conf"));
        ActorSystem.create("conf", config).shutdown();
    }

    @Test
    public void async() throws Exception {
        Application app = fakeApplication();
        running(app, () -> {
            Result result = MockJavaActionHelper.call(new MockJavaAction() {
                public CompletionStage<Result> index() {
                    return new javaguide.akka.async.Application().index();
                }
            }, fakeRequest(), app.getWrappedApplication().materializer());
            assertThat(contentAsString(result), equalTo("Got 2"));
        });
    }

    @Test
    public void scheduleActor() throws Exception {
        Application app = fakeApplication();
        running(app, () -> {
            ActorSystem system = app.injector().instanceOf(ActorSystem.class);
            latch = new CountDownLatch(1);
            ActorRef testActor = system.actorOf(Props.create(MyActor.class));
            //#schedule-actor
            system.scheduler().schedule(
                    Duration.create(0, TimeUnit.MILLISECONDS), //Initial delay 0 milliseconds
                    Duration.create(30, TimeUnit.MINUTES),     //Frequency 30 minutes
                    testActor,
                    "tick",
                    system.dispatcher(),
                    null
            );
            //#schedule-actor
            try {
                assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void scheduleCode() throws Exception {
        Application app = fakeApplication();
        running(app, () -> {

            ActorSystem system = app.getWrappedApplication().injector().instanceOf(ActorSystem.class);
            final CountDownLatch latch = new CountDownLatch(1);
            class MockFile {
                void delete() {
                    latch.countDown();
                }
            }
            final MockFile file = new MockFile();
            //#schedule-code
            system.scheduler().scheduleOnce(
                    Duration.create(10, TimeUnit.MILLISECONDS),
                    () -> file.delete(),
                    system.dispatcher()
            );
            //#schedule-code
            try {
                assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void customerSimpleMaterializerProvider() throws Exception {
        Application app = new GuiceApplicationBuilder()
                .configure("play.akka.materializer.provider", "javaguide.akka.SimpleMaterializerProvider")
                .build();
        running(app, () -> {
            SimpleMaterializerProvider provider = app.injector().instanceOf(SimpleMaterializerProvider.class);
            Materializer mat = app.injector().instanceOf(Materializer.class);
            try {
                Source.single(1).map(t -> {
                    if (t == 1) {
                        throw new RuntimeException();
                    } else {
                        return t;
                    }
                }).toMat(Sink.foreach(System.out::println), Keep.right()).run(mat).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException ignored) {
                // Ignore errors like a Materialized Graph
            }
            assertEquals(1, provider.getErrorCount());
        });
    }

}
