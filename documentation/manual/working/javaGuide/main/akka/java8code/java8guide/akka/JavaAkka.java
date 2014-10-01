/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.akka;

import akka.actor.*;
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

    @Test
    public void ask() throws Exception {
        java8guide.akka.ask.Application controller = app.getWrappedApplication().injector().instanceOf(java8guide.akka.ask.Application.class);

        String message = Helpers.contentAsString(controller.sayHello("world").get(1000));
        assertThat(message, equalTo("Hello, world"));
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
