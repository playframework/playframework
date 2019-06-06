/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.httprequesthandlers;

// #simple
import javax.inject.Inject;

import play.core.j.JavaContextComponents;
import play.routing.Router;
import play.api.mvc.Handler;
import play.http.*;
import play.mvc.*;
import play.libs.streams.Accumulator;
import play.core.j.JavaHandler;
import play.core.j.JavaHandlerComponents;

public class SimpleHttpRequestHandler implements HttpRequestHandler {
  private final Router router;
  private final JavaHandlerComponents handlerComponents;

  @Inject
  public SimpleHttpRequestHandler(Router router, JavaHandlerComponents components) {
    this.router = router;
    this.handlerComponents = components;
  }

  public HandlerForRequest handlerForRequest(Http.RequestHeader request) {
    Handler handler =
        router
            .route(request)
            .orElseGet(() -> EssentialAction.of(req -> Accumulator.done(Results.notFound())));
    if (handler instanceof JavaHandler) {
      handler = ((JavaHandler) handler).withComponents(handlerComponents);
    }
    return new HandlerForRequest(request, handler);
  }
}
// #simple
