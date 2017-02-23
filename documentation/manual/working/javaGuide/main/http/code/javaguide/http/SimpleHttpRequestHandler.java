/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.httprequesthandlers;

//#simple
import javax.inject.Inject;
import play.routing.Router;
import play.api.mvc.Handler;
import play.http.*;
import play.mvc.*;
import play.libs.streams.Accumulator;
import play.core.j.JavaHandler;
import play.core.j.JavaHandlerComponents;

public class SimpleHttpRequestHandler implements HttpRequestHandler {
    private final Router router;
    private final JavaHandlerComponents components;

    @Inject
    public SimpleHttpRequestHandler(Router router, JavaHandlerComponents components) {
        this.router = router;
        this.components = components;
    }

    public HandlerForRequest handlerForRequest(Http.RequestHeader request) {
        Handler handler = router.route(request).orElseGet(() ->
            EssentialAction.of(req -> Accumulator.done(Results.notFound()))
        );
        if (handler instanceof JavaHandler) {
            handler = ((JavaHandler)handler).withComponents(components);
        }
        return new HandlerForRequest(request, handler);
    }
}
//#simple
