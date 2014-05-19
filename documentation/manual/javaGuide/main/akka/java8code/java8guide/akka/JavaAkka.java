/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.akka;

import akka.actor.*;
import akka.actor.Props;
import com.typesafe.config.*;
import javaguide.testhelpers.MockJavaAction;
import org.junit.Before;
import org.junit.Test;
import play.libs.Akka;
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
                () -> file.delete(),
                Akka.system().dispatcher()
        );
        //#schedule-code
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

}
