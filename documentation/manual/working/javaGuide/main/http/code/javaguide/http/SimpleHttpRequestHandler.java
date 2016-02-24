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

public class SimpleHttpRequestHandler implements HttpRequestHandler {
    private final Router router;

    @Inject
    public SimpleHttpRequestHandler(Router router) {
        this.router = router;
    }

    public HandlerForRequest handlerForRequest(Http.RequestHeader request) {
        Handler handler = router.route(request).orElseGet(() ->
            EssentialAction.of(req -> Accumulator.done(Results.notFound()))
        );
        return new HandlerForRequest(request, handler);
    }
}
//#simple
