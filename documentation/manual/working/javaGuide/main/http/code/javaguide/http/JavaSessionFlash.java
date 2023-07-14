/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http;

import akka.stream.Materializer;
import org.junit.jupiter.api.*;
import play.core.j.JavaHandlerComponents;
import javaguide.testhelpers.MockJavaAction;
import play.Application;
import play.test.junit5.ApplicationExtension;


// #imports
import play.mvc.*;
import play.mvc.Http.*;
// #imports

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

public class JavaSessionFlash {
    static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
    static Application app = appExtension.getApplication();
    static Materializer mat = appExtension.getMaterializer();
  @Test
   void readSession() {
    assertEquals(
            "Hello foo",
        contentAsString(
            call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #read-session
                  public Result index(Http.Request request) {
                    return request
                        .session()
                        .get("connected")
                        .map(user -> ok("Hello " + user))
                        .orElseGet(() -> unauthorized("Oops, you are not connected"));
                  }
                  // #read-session
                },
                fakeRequest().session("connected", "foo"),
                mat)));
  }

  @Test
   void storeSession() {
    Session session =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #store-session
                  public Result login(Http.Request request) {
                    return redirect("/home")
                        .addingToSession(request, "connected", "user@gmail.com");
                  }
                  // #store-session
                },
                fakeRequest(),
                mat)
            .session();
    assertEquals("user@gmail.com", session.get("connected").get());
  }

  @Test
   void removeFromSession() {
    Session session =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #remove-from-session
                  public Result logout(Http.Request request) {
                    return redirect("/home").removingFromSession(request, "connected");
                  }
                  // #remove-from-session
                },
                fakeRequest().session("connected", "foo"),
                mat)
            .session();
    assertFalse(session.get("connected").isPresent());
  }

  @Test
   void discardWholeSession() {
    Session session =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #discard-whole-session
                  public Result logout() {
                    return redirect("/home").withNewSession();
                  }
                  // #discard-whole-session
                },
                fakeRequest().session("connected", "foo"),
                mat)
            .session();
    assertFalse(session.get("connected").isPresent());
  }

  @Test
   void readFlash() {
    assertEquals(
            "hi",
        contentAsString(
            call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #read-flash
                  public Result index(Http.Request request) {
                    return ok(request.flash().get("success").orElse("Welcome!"));
                  }
                  // #read-flash
                },
                fakeRequest().flash("success", "hi"),
                mat)));
  }

  @Test
   void storeFlash() {
    Flash flash =
        call(
                new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
                  // #store-flash
                  public Result save() {
                    return redirect("/home").flashing("success", "The item has been created");
                  }
                  // #store-flash
                },
                fakeRequest(),
                mat)
            .flash();
    assertEquals("The item has been created", flash.get("success").get());
  }

  @Test
   void accessFlashInTemplate() {
    MockJavaAction index =
        new MockJavaAction(app.injector().instanceOf(JavaHandlerComponents.class)) {
          public Result index(Http.Request request) {
            return ok(javaguide.http.views.html.index.render(request.flash()));
          }
        };
    assertEquals("Welcome!", contentAsString(call(index, fakeRequest(), mat)).trim());
    assertEquals("Flashed!", contentAsString(call(index, fakeRequest().flash("success", "Flashed!"), mat)).trim());
  }
}
