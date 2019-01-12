/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import org.junit.*;
import play.core.j.JavaHandlerComponents;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;

//#imports
import play.mvc.*;
import play.mvc.Http.*;
//#imports

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaSessionFlash extends WithApplication {

    @Test
    public void readSession() {
        assertThat(contentAsString(call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                    //#read-session
                    public Result index(Http.Request request) {
                        return request.session()
                            .getOptional("connected")
                            .map(user -> ok("Hello " + user))
                            .orElseGet(() -> unauthorized("Oops, you are not connected"));
                    }
                    //#read-session
                }, fakeRequest().session("connected", "foo"), mat)),
                equalTo("Hello foo"));
    }

    @Test
    public void storeSession() {
        Session session = call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
            //#store-session
            public Result login(Http.Request request) {
                return ok("Welcome!").addingToSession(request, "connected", "user@gmail.com");
            }
            //#store-session
        }, fakeRequest(), mat).session();
        assertThat(session.getOptional("connected").get(), equalTo("user@gmail.com"));
    }

    @Test
    public void removeFromSession() {
        Session session = call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
            //#remove-from-session
            public Result logout(Http.Request request) {
                return ok("Bye").removingFromSession(request, "connected");
            }
            //#remove-from-session
        }, fakeRequest().session("connected", "foo"), mat).session();
        assertFalse(session.getOptional("connected").isPresent());
    }

    @Test
    public void discardWholeSession() {
        Session session = call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
            //#discard-whole-session
            public Result logout() {
                return ok("Bye").withNewSession();
            }
            //#discard-whole-session
        }, fakeRequest().session("connected", "foo"), mat).session();
        assertFalse(session.getOptional("connected").isPresent());
    }

    @Test
    public void readFlash() {
        assertThat(contentAsString(call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                    //#read-flash
                    public Result index(Http.Request request) {
                        return ok(request.flash().getOptional("success").orElse("Welcome!"));
                    }
                    //#read-flash
                }, fakeRequest().flash("success", "hi"), mat)),
                equalTo("hi"));
    }

    @Test
    public void storeFlash() {
        Flash flash = call(new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
            //#store-flash
            public Result save() {
                return redirect("/home").flashing("success", "The item has been created");
            }
            //#store-flash
        }, fakeRequest(), mat).flash();
        assertThat(flash.getOptional("success").get(), equalTo("The item has been created"));
    }

    @Test
    public void accessFlashInTemplate() {
        MockJavaAction index = new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
            public Result index(Http.Request request) {
                return ok(javaguide.http.views.html.index.render(request.flash()));
            }
        };
        assertThat(contentAsString(call(index, fakeRequest(), mat)).trim(), equalTo("Welcome!"));
        assertThat(contentAsString(call(index, fakeRequest().flash("success", "Flashed!"), mat)).trim(), equalTo("Flashed!"));
    }
}
