/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

import akka.actor.*;
import com.typesafe.config.*;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Test;

import play.libs.F.Promise;
import play.mvc.Result;
import play.test.WithApplication;
import play.test.Helpers;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JavaAkka extends WithApplication {

    private static volatile CountDownLatch latch;
    public static class MyActor extends UntypedActor {
        @Override
        public void onReceive(Object msg) throws Exception {
            latch.countDown();
        }
    }

    @Test
    public void ask() throws Exception {
        javaguide.akka.ask.Application controller = app.getWrappedApplication().injector().instanceOf(javaguide.akka.ask.Application.class);

        String message = Helpers.contentAsString(controller.sayHello("world").get(1000));
        assertThat(message, equalTo("Hello, world"));
    }

    @Test
    public void conf() throws Exception {
        Config config = ConfigFactory.parseURL(getClass().getResource("akka.conf"));
        ActorSystem.create("conf", config).shutdown();
    }

    @Test
    public void async() throws Exception {
        Result result = MockJavaActionHelper.call(new MockJavaAction() {
            public Promise<Result> index() {
                return new javaguide.akka.async.Application().index();
            }
        }, Helpers.fakeRequest());
        assertThat(Helpers.contentAsString(result), equalTo("Got 2"));
    }

    @Test
    public void scheduleActor() throws Exception {
        ActorSystem system = app.getWrappedApplication().injector().instanceOf(ActorSystem.class);
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
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleCode() throws Exception {
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
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

}
