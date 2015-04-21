/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

import akka.actor.*;
import static akka.pattern.Patterns.ask;
import com.typesafe.config.*;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Test;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.F.Promise;
import play.mvc.Result;
import play.test.*;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
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

            String message = contentAsString(controller.sayHello("world").get(1000));
            assertThat(message, equalTo("Hello, world"));
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

            String message = contentAsString(controller.getConfig().get(1000));
            assertThat(message, equalTo("foo"));
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

            String message = (String) Promise.wrap(ask(parent, new ParentActorProtocol.GetChild("my.config"), 1000)).flatMap(child ->
                    Promise.wrap(ask((ActorRef) child, new ConfiguredChildActorProtocol.GetConfig(), 1000))
            ).get(5000);

            assertThat(message, equalTo("foo"));
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
                public Promise<Result> index() {
                    return new javaguide.akka.async.Application().index();
                }
            }, fakeRequest());
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

}
