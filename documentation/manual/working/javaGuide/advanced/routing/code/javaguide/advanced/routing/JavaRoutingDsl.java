/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.routing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// #imports
import javax.inject.Inject;

import play.api.mvc.AnyContent;
import play.api.mvc.BodyParser;
import play.api.mvc.PlayBodyParsers;
import play.mvc.Http;
import play.routing.Router;
import play.routing.RoutingDsl;
import java.util.concurrent.CompletableFuture;

import static play.mvc.Controller.*;
// #imports

import play.mvc.Result;
import play.test.junit5.ApplicationExtension;
import play.Application;

import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

public class JavaRoutingDsl {

  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
  static Application app = appExtension.getApplication();
  static RoutingDsl routingDsl;

  @BeforeAll
  static void initializeRoutingDsl() {
    routingDsl = app.injector().instanceOf(RoutingDsl.class);
  }

  @Test
  void simple() {
    // #simple
    Router router = routingDsl.GET("/hello/:to").routingTo((request, to) -> ok("Hello " + to)).build();
    // #simple

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
  }

  @Test
  void fullPath() {
    // #full-path
    Router router = routingDsl.GET("/assets/*file").routingTo((request, file) -> ok("Serving " + file)).build();
    // #full-path

    assertEquals("Serving javascripts/main.js", makeRequest(router, "GET", "/assets/javascripts/main.js"));
  }

  @Test
  void regexp() {
    // #regexp
    Router router =
        routingDsl
            .GET("/api/items/$id<[0-9]+>")
            .routingTo((request, id) -> ok("Getting item " + id))
            .build();
    // #regexp

    assertEquals("Getting item 23", makeRequest(router, "GET", "/api/items/23"));
  }

  @Test
  void integer() {
    // #integer
    Router router =
        routingDsl
            .GET("/api/items/:id")
            .routingTo((Http.Request request, Integer id) -> ok("Getting item " + id))
            .build();
    // #integer

    assertEquals("Getting item 23", makeRequest(router, "GET", "/api/items/23"));
  }

  @Test
  void async() {
    // #async
    Router router =
        routingDsl
            .GET("/api/items/:id")
            .routingAsync(
                (Http.Request request, Integer id) ->
                    CompletableFuture.completedFuture(ok("Getting item " + id)))
            .build();
    // #async

    assertEquals("Getting item 23", makeRequest(router, "GET", "/api/items/23"));
  }

  private String makeRequest(Router router, String method, String path) {
    Result result = routeAndCall(app, router, fakeRequest(method, path));
    if (result == null) {
      return null;
    } else {
      return contentAsString(result);
    }
  }

  // #inject
  public class MyComponent {

    private final RoutingDsl routingDsl;

    @Inject
    public MyComponent(RoutingDsl routing) {
      this.routingDsl = routing;
    }
  }
  // #inject

  @Test
  void createNewRoutingDsl() {
    play.mvc.BodyParser.Default bodyParser =
        app.injector().instanceOf(play.mvc.BodyParser.Default.class);

    // #new-routing-dsl
    RoutingDsl routingDsl = new RoutingDsl(bodyParser);
    // #new-routing-dsl
    Router router =
        routingDsl.GET("/hello/:to").routingTo((request, to) -> ok("Hello " + to)).build();

    assertEquals("Hello world", makeRequest(router, "GET", "/hello/world"));
  }
}
