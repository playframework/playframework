/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters;

import static play.test.Helpers.GET;
import static play.test.Helpers.POST;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.api.test.CSRFTokenHelper;
import play.mvc.Http;
import play.mvc.Results;
import play.routing.Router;
import play.routing.RoutingDsl;
import play.test.Helpers;
import play.test.junit5.ApplicationExtension;

public class FiltersTest {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(Helpers.fakeApplication());

  static Application app = appExtension.getApplication();

  @Test
  void testRequestBuilder() {
    Router router =
        new RoutingDsl(app.injector().instanceOf(play.mvc.BodyParser.Default.class))
            .GET("/xx/Kiwi")
            .routingTo(request -> Results.ok("success"))
            .build();

    // #test-with-request-builder
    Http.RequestBuilder request =
        new Http.RequestBuilder()
            .method(GET)
            .header(Http.HeaderNames.HOST, "localhost")
            .uri("/xx/Kiwi");
    // #test-with-request-builder

    Helpers.routeAndCall(app, router, request, 10_000 /* 10 seconds */);
  }

  @Test
  void test() {
    Router router =
        new RoutingDsl(app.injector().instanceOf(play.mvc.BodyParser.Default.class))
            .POST("/xx/Kiwi")
            .routingTo(request -> Results.ok("success"))
            .build();

    // #test-with-addCSRFToken
    Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri("/xx/Kiwi");

    request = CSRFTokenHelper.addCSRFToken(request);
    // #test-with-addCSRFToken

    Helpers.routeAndCall(app, router, request, 10_000 /* 10 seconds */);
  }
}
