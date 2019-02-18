/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.routing;

import org.junit.Before;
import org.junit.Test;

// #imports
import javax.inject.Inject;

import play.api.mvc.AnyContent;
import play.api.mvc.BodyParser;
import play.api.mvc.PlayBodyParsers;
import play.core.j.JavaContextComponents;
import play.routing.Router;
import play.routing.RoutingDsl;
import java.util.concurrent.CompletableFuture;

import static play.mvc.Controller.*;
// #imports

import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaRoutingDsl extends WithApplication {

  private RoutingDsl routingDsl;

  @Before
  public void initializeRoutingDsl() {
    this.routingDsl = app.injector().instanceOf(RoutingDsl.class);
  }

  @Test
  public void simple() {
    // #simple
    Router router = routingDsl.GET("/hello/:to").routeTo(to -> ok("Hello " + to)).build();
    // #simple

    assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
  }

  @Test
  public void fullPath() {
    // #full-path
    Router router = routingDsl.GET("/assets/*file").routeTo(file -> ok("Serving " + file)).build();
    // #full-path

    assertThat(
        makeRequest(router, "GET", "/assets/javascripts/main.js"),
        equalTo("Serving javascripts/main.js"));
  }

  @Test
  public void regexp() {
    // #regexp
    Router router =
        routingDsl.GET("/api/items/$id<[0-9]+>").routeTo(id -> ok("Getting item " + id)).build();
    // #regexp

    assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
  }

  @Test
  public void integer() {
    // #integer
    Router router =
        routingDsl.GET("/api/items/:id").routeTo((Integer id) -> ok("Getting item " + id)).build();
    // #integer

    assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
  }

  @Test
  public void async() {
    // #async
    Router router =
        routingDsl
            .GET("/api/items/:id")
            .routeAsync((Integer id) -> CompletableFuture.completedFuture(ok("Getting item " + id)))
            .build();
    // #async

    assertThat(makeRequest(router, "GET", "/api/items/23"), equalTo("Getting item 23"));
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
  public void createNewRoutingDsl() {
    play.mvc.BodyParser.Default bodyParser =
        app.injector().instanceOf(play.mvc.BodyParser.Default.class);
    JavaContextComponents javaContextComponents =
        app.injector().instanceOf(JavaContextComponents.class);

    // #new-routing-dsl
    RoutingDsl routingDsl = new RoutingDsl(bodyParser, javaContextComponents);
    // #new-routing-dsl
    Router router = routingDsl.GET("/hello/:to").routeTo(to -> ok("Hello " + to)).build();

    assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
  }
}
