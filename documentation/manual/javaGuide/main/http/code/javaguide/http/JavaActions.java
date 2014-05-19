/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Test;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.test.WithApplication;
import play.test.Helpers;

import javaguide.testhelpers.MockJavaAction;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static javaguide.testhelpers.MockJavaAction.call;

public class JavaActions {
    @Test
    public void simpleAction() {
        assertThat(status(call(new Controller1(), fakeRequest())), equalTo(200));
    }

    public static class Controller1 extends MockJavaAction {
        //#simple-action
        public static Result index() {
            return ok("Got request " + request() + "!");
        }
        //#simple-action
    }

    @Test
    public void fullController() {
        assertThat(status(call(new Controller2(), fakeRequest())), equalTo(200));
    }

    public static class Controller2 extends MockJavaAction {
        public static Result index() {
            return javaguide.http.full.Application.index();
        }
    }

    @Test
    public void withParams() {
        Result result = call(new Controller3(), fakeRequest());
        assertThat(status(result), equalTo(200));
        assertThat(contentAsString(result), equalTo("Hello world"));
    }

    static class Controller3 extends MockJavaAction {
        //#params-action
        public static Result index(String name) {
            return ok("Hello " + name);
        }
        //#params-action

        public F.Promise<Result> invocation() {
            return F.Promise.pure(index("world"));
        }
    }

    @Test
    public void simpleResult() {
        assertThat(status(call(new Controller4(), fakeRequest())), equalTo(200));
    }

    public static class Controller4 extends MockJavaAction {
        //#simple-result
        public static Result index() {
            return ok("Hello world!");
        }
        //#simple-result
    }

    @Test
    public void otherResults() {
        // Mock the existence of a view...
        class Form {
            String render(Object o) {
                return "";
            }
        }
        class Html {
            Form form = new Form();
        }
        class Views {
            Html html = new Html();
        }
        final Views views = new Views();

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

    @Test
    public void redirectAction() {
        Result result = call(new Controller6(), fakeRequest());
        assertThat(status(result), equalTo(SEE_OTHER));
        assertThat(header(LOCATION, result), equalTo("/user/home"));
    }

    public static class Controller6 extends MockJavaAction {
        //#redirect-action
        public static Result index() {
            return redirect("/user/home");
        }
        //#redirect-action
    }

    @Test
    public void temporaryRedirectAction() {
        Result result = call(new Controller7(), fakeRequest());
        assertThat(status(result), equalTo(TEMPORARY_REDIRECT));
        assertThat(header(LOCATION, result), equalTo("/user/home"));
    }

    public static class Controller7 extends MockJavaAction {
        //#temporary-redirect-action
        public static Result index() {
            return temporaryRedirect("/user/home");
        }
        //#temporary-redirect-action
    }


}
