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

import static javaguide.testhelpers.MockJavaActionHelper.*;
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
        assertThat(contentAsString(call(new MockJavaAction() {
                    //#read-session
                    public Result index() {
                        String user = session("connected");
                        if(user != null) {
                            return ok("Hello " + user);
                        } else {
                            return unauthorized("Oops, you are not connected");
                        }
                    }
                    //#read-session
                }, fakeRequest().withSession("connected", "foo"))),
                equalTo("Hello foo"));
    }

    @Test
    public void storeSession() {
        Session session = session(call(new MockJavaAction() {
            //#store-session
            public Result login() {
                session("connected", "user@gmail.com");
                return ok("Welcome!");
            }
            //#store-session
        }, fakeRequest()));
        assertThat(session.get("connected"), equalTo("user@gmail.com"));
    }

    @Test
    public void removeFromSession() {
        Session session = session(call(new MockJavaAction() {
            //#remove-from-session
            public Result logout() {
                session().remove("connected");
                return ok("Bye");
            }
            //#remove-from-session
        }, fakeRequest().withSession("connected", "foo")));
        assertThat(session.get("connected"), nullValue());
    }

    @Test
    public void discardWholeSession() {
        Session session = session(call(new MockJavaAction() {
            //#discard-whole-session
            public Result logout() {
                session().clear();
                return ok("Bye");
            }
            //#discard-whole-session
        }, fakeRequest().withSession("connected", "foo")));
        assertThat(session.get("connected"), nullValue());
    }

    @Test
    public void readFlash() {
        assertThat(contentAsString(call(new MockJavaAction() {
                    //#read-flash
                    public Result index() {
                        String message = flash("success");
                        if(message == null) {
                            message = "Welcome!";
                        }
                        return ok(message);
                    }
                    //#read-flash
                }, fakeRequest().withFlash("success", "hi"))),
                equalTo("hi"));
    }

    @Test
    public void storeFlash() {
        Flash flash = flash(call(new MockJavaAction() {
            //#store-flash
            public Result save() {
                flash("success", "The item has been created");
                return redirect("/home");
            }
            //#store-flash
        }, fakeRequest()));
        assertThat(flash.get("success"), equalTo("The item has been created"));
    }

    @Test
    public void accessFlashInTemplate() {
        MockJavaAction index = new MockJavaAction() {
            public Result index() {
                return ok(javaguide.http.views.html.index.render());
            }
        };
        assertThat(contentAsString(call(index, fakeRequest())).trim(), equalTo("Welcome!"));
        assertThat(contentAsString(call(index, fakeRequest().withFlash("success", "Flashed!"))).trim(), equalTo("Flashed!"));
    }
}
