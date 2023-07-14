/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import akka.stream.Materializer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javaguide.testhelpers.MockJavaAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.core.j.JavaHandlerComponents;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.test.junit5.ApplicationExtension;

public class JavaActions {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());

  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  @Test
  void simpleAction() {
    assertEquals(
        200,
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #simple-action
                  public Result index(Http.Request request) {
                    return ok("Got request " + request + "!");
                  }
                  // #simple-action
                },
                fakeRequest(),
                mat)
            .status());
  }

  @Test
  void fullController() {
    assertEquals(
        200,
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  public Result index() {
                    return new javaguide.http.full.Application().index();
                  }
                },
                fakeRequest(),
                mat)
            .status());
  }

  @Test
  void withParams() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #params-action
              public Result index(String name) {
                return ok("Hello " + name);
              }
              // #params-action

              public CompletionStage<Result> invocation() {
                return CompletableFuture.completedFuture(index("world"));
              }
            },
            fakeRequest(),
            mat);
    assertEquals(200, result.status());
    assertEquals("Hello world", contentAsString(result));
  }

  @Test
  void simpleResult() {
    assertEquals(
        200,
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #simple-result
                  public Result index() {
                    return ok("Hello world!");
                  }
                  // #simple-result
                },
                fakeRequest(),
                mat)
            .status());
  }

  @Test
  void otherResults() {

    class Controller5 extends Controller {
      void run() {
        Object formWithErrors = null;

        // #other-results
        Result ok = ok("Hello world!");
        Result notFound = notFound();
        Result pageNotFound = notFound("<h1>Page not found</h1>").as("text/html");
        Result badRequest = badRequest(views.html.form.render(formWithErrors));
        Result oops = internalServerError("Oops");
        Result anyStatus = status(488, "Strange response type");
        // #other-results

        assertEquals(488, anyStatus.status());
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
  void redirectAction() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #redirect-action
              public Result index() {
                return redirect("/user/home");
              }
              // #redirect-action
            },
            fakeRequest(),
            mat);
    assertEquals(SEE_OTHER, result.status());
    assertEquals(Optional.of("/user/home"), result.header(LOCATION));
  }

  @Test
  void temporaryRedirectAction() {
    Result result =
        call(
            new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
              // #temporary-redirect-action
              public Result index() {
                return temporaryRedirect("/user/home");
              }
              // #temporary-redirect-action
            },
            fakeRequest(),
            mat);
    assertEquals(TEMPORARY_REDIRECT, result.status());
    assertEquals(Optional.of("/user/home"), result.header(LOCATION));
  }
}
