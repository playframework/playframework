/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

import com.typesafe.config.*;
import javaguide.testhelpers.MockJavaAction;
import org.junit.Test;

//#akka-imports
import akka.actor.*;
import play.libs.Akka;
//#akka-imports

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
    
    //#akka-MyActor
    public static class MyActor extends UntypedActor {
        @Override
        public void onReceive(Object msg) throws Exception {
            //###replace:         // receive logic
            latch.countDown();
        }
    }
    //#akka-MyActor
  
    @Test
    public void actorFor() throws Exception {
        //#akka-actorOf
        ActorRef myActor = Akka.system().actorOf(Props.create(MyActor.class));
        //#akka-actorOf

        latch = new CountDownLatch(1);
        myActor.tell("hello", null);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }    

    @Test
    public void conf() throws Exception {
        Config config = ConfigFactory.parseURL(getClass().getResource("akka.conf"));
        ActorSystem.create("conf", config).shutdown();
    }

    @Test
    public void ask() throws Exception {
        Akka.system().actorOf(Props.create(EchoActor.class), "my-actor");
        Result result = MockJavaAction.call(new MockJavaAction() {
            public Promise<Result> index() {
                return javaguide.akka.ask.Application.index();
            }
        }, Helpers.fakeRequest());
        assertThat(Helpers.contentAsString(result), equalTo("got hello"));
    }

    public static class EchoActor extends UntypedActor {
        @Override
        public void onReceive(Object msg) throws Exception {
            sender().tell("got " + msg, null);
        }
    }

    @Test
    public void async() throws Exception {
        Result result = MockJavaAction.call(new MockJavaAction() {
            public Promise<Result> index() {
                return javaguide.akka.async.Application.index();
            }
        }, Helpers.fakeRequest());
        assertThat(Helpers.contentAsString(result), equalTo("Got 2"));
    }

    @Test
    public void scheduleActor() throws Exception {
        latch = new CountDownLatch(1);
        ActorRef testActor = Akka.system().actorOf(Props.create(MyActor.class));
        //#schedule-actor
        Akka.system().scheduler().schedule(
                Duration.create(0, TimeUnit.MILLISECONDS), //Initial delay 0 milliseconds
                Duration.create(30, TimeUnit.MINUTES),     //Frequency 30 minutes
                testActor,
                "tick",
                Akka.system().dispatcher(),
                null
        );
        //#schedule-actor
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleCode() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        class MockFile {
            void delete() {
                latch.countDown();
            }
        }
        final MockFile file = new MockFile();
        //#schedule-code
        Akka.system().scheduler().scheduleOnce(
                Duration.create(10, TimeUnit.MILLISECONDS),
                new Runnable() {
                    public void run() {
                        file.delete();
                    }
                },
                Akka.system().dispatcher()
        );
        //#schedule-code
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

}
