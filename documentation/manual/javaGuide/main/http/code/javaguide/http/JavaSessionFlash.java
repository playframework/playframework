/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import com.google.common.collect.ImmutableMap;
import org.junit.*;
import play.libs.Json;
import play.test.WithApplication;
import play.test.FakeApplication;
import javaguide.testhelpers.MockJavaAction;

//#imports
import play.mvc.*;
import play.mvc.Http.*;
//#imports

import static javaguide.testhelpers.MockJavaAction.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaSessionFlash extends WithApplication {

    @Override
    public FakeApplication provideFakeApplication() {
        return fakeApplication(ImmutableMap.of("application.secret", "pass"));
    }

    @Test
    public void readSession() {
        assertThat(contentAsString(call(new Controller1(), fakeRequest().withSession("connected", "foo"))),
                equalTo("Hello foo"));
    }

    public static class Controller1 extends MockJavaAction {
        //#read-session
        public static Result index() {
            String user = session("connected");
            if(user != null) {
                return ok("Hello " + user);
            } else {
                return unauthorized("Oops, you are not connected");
            }
        }
        //#read-session
    }

    @Test
    public void storeSession() {
        Session session = session(call(new Controller2(), fakeRequest()));
        assertThat(session.get("connected"), equalTo("user@gmail.com"));
    }

    public static class Controller2 extends MockJavaAction {
        //#store-session
        public static Result login() {
            session("connected", "user@gmail.com");
            return ok("Welcome!");
        }
        //#store-session
    }

    @Test
    public void removeFromSession() {
        Session session = session(call(new Controller3(), fakeRequest().withSession("connected", "foo")));
        assertThat(session.get("connected"), nullValue());
    }

    public static class Controller3 extends MockJavaAction {
        //#remove-from-session
        public static Result logout() {
            session().remove("connected");
            return ok("Bye");
        }
        //#remove-from-session
    }

    @Test
    public void discardWholeSession() {
        Session session = session(call(new Controller4(), fakeRequest().withSession("connected", "foo")));
        assertThat(session.get("connected"), nullValue());
    }

    public static class Controller4 extends MockJavaAction {
        //#discard-whole-session
        public static Result logout() {
            session().clear();
            return ok("Bye");
        }
        //#discard-whole-session
    }

    @Test
    public void readFlash() {
        assertThat(contentAsString(call(new Controller5(), fakeRequest().withFlash("success", "hi"))),
                equalTo("hi"));
    }

    public static class Controller5 extends MockJavaAction {
        //#read-flash
        public static Result index() {
            String message = flash("success");
            if(message == null) {
                message = "Welcome!";
            }
            return ok(message);
        }
        //#read-flash
    }

    @Test
    public void storeFlash() {
        Flash flash = flash(call(new Controller6(), fakeRequest()));
        assertThat(flash.get("success"), equalTo("The item has been created"));
    }

    public static class Controller6 extends MockJavaAction {
        //#store-flash
        public static Result save() {
            flash("success", "The item has been created");
            return redirect("/home");
        }
        //#store-flash
    }
}
