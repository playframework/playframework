/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters;

import org.junit.Test;
import play.api.mvc.PlayBodyParsers;
import play.api.test.CSRFTokenHelper;
import play.core.j.JavaContextComponents;
import play.mvc.Http;
import play.mvc.Results;
import play.routing.Router;
import play.routing.RoutingDsl;
import play.test.Helpers;
import play.test.WithApplication;

import static play.test.Helpers.GET;
import static play.test.Helpers.POST;

public class FiltersTest extends WithApplication {

  @Test
  public void testRequestBuilder() {
    Router router =
        new RoutingDsl(
                instanceOf(play.mvc.BodyParser.Default.class),
                instanceOf(JavaContextComponents.class))
            .GET("/xx/Kiwi")
            .routeTo(() -> Results.ok("success"))
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
  public void test() {
    Router router =
        new RoutingDsl(
                instanceOf(play.mvc.BodyParser.Default.class),
                instanceOf(JavaContextComponents.class))
            .POST("/xx/Kiwi")
            .routeTo(() -> Results.ok("success"))
            .build();

    // #test-with-addCSRFToken
    Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri("/xx/Kiwi");

    request = CSRFTokenHelper.addCSRFToken(request);
    // #test-with-addCSRFToken

    Helpers.routeAndCall(app, router, request, 10_000 /* 10 seconds */);
  }
}
