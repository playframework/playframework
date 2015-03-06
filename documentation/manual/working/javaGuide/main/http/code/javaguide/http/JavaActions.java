/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
        assertThat(call(new MockJavaAction() {
            //#simple-action
            public Result index() {
                return ok("Got request " + request() + "!");
            }
            //#simple-action
        }, fakeRequest()).status(), equalTo(200));
    }

    @Test
    public void fullController() {
        assertThat(call(new MockJavaAction() {
            public Result index() {
                return new javaguide.http.full.Application().index();
            }
        }, fakeRequest()).status(), equalTo(200));
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
        assertThat(result.status(), equalTo(200));
        assertThat(contentAsString(result), equalTo("Hello world"));
    }

    @Test
    public void simpleResult() {
        assertThat(call(new MockJavaAction() {
            //#simple-result
            public Result index() {
                return ok("Hello world!");
            }
            //#simple-result
        }, fakeRequest()).status(), equalTo(200));
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

                assertThat(anyStatus.status(), equalTo(488));
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
        assertThat(result.status(), equalTo(SEE_OTHER));
        assertThat(result.header(LOCATION), equalTo("/user/home"));
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
        assertThat(result.status(), equalTo(TEMPORARY_REDIRECT));
        assertThat(result.header(LOCATION), equalTo("/user/home"));
    }

}
