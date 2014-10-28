/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Test;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.test.Helpers;

import javaguide.testhelpers.MockJavaAction;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static javaguide.testhelpers.MockJavaActionHelper.call;

public class JavaActions extends WithApplication {
    @Test
    public void simpleAction() {
        assertThat(status(call(new MockJavaAction() {
            //#simple-action
            public Result index() {
                return ok("Got request " + request() + "!");
            }
            //#simple-action
        }, fakeRequest())), equalTo(200));
    }

    @Test
    public void fullController() {
        assertThat(status(call(new MockJavaAction() {
            public Result index() {
                return new javaguide.http.full.Application().index();
            }
        }, fakeRequest())), equalTo(200));
    }

    @Test
    public void withParams() {
        Result result = call(new MockJavaAction() {
            //#params-action
            public Result index(String name) {
                return ok("Hello " + name);
            }
            //#params-action

            public F.Promise<Result> invocation() {
                return F.Promise.pure(index("world"));
            }
        }, fakeRequest());
        assertThat(status(result), equalTo(200));
        assertThat(contentAsString(result), equalTo("Hello world"));
    }

    @Test
    public void simpleResult() {
        assertThat(status(call(new MockJavaAction() {
            //#simple-result
            public Result index() {
                return ok("Hello world!");
            }
            //#simple-result
        }, fakeRequest())), equalTo(200));
    }

    @Test
    public void otherResults() {

        class Controller5 extends Controller {
            void run() {
                Object formWithErrors = null;

                //#other-results
                Result ok = ok("Hello world!");
                Result notFound = notFound();
                Result pageNotFound = notFound("<h1>Page not found</h1>").as("text/html");
                Result badRequest = badRequest(views.html.form.render(formWithErrors));
                Result oops = internalServerError("Oops");
                Result anyStatus = status(488, "Strange response type");
                //#other-results

                assertThat(Helpers.status(anyStatus), equalTo(488));
            }
        }

        new Controller5().run();
    }

    // Mock the existence of a view...
    static class views {
        static class html {
            static class form {
                static String render(Object o) {
                    return "";
                }
            }
        }
    }

    @Test
    public void redirectAction() {
        Result result = call(new MockJavaAction() {
            //#redirect-action
            public Result index() {
                return redirect("/user/home");
            }
            //#redirect-action
        }, fakeRequest());
        assertThat(status(result), equalTo(SEE_OTHER));
        assertThat(header(LOCATION, result), equalTo("/user/home"));
    }

    @Test
    public void temporaryRedirectAction() {
        Result result = call(new MockJavaAction() {
            //#temporary-redirect-action
            public Result index() {
                return temporaryRedirect("/user/home");
            }
            //#temporary-redirect-action
        }, fakeRequest());
        assertThat(status(result), equalTo(TEMPORARY_REDIRECT));
        assertThat(header(LOCATION, result), equalTo("/user/home"));
    }

}