/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import play.core.j.JavaHttpRequestHandlerAdapter;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

/**
 * An HTTP request handler
 */
public interface HttpRequestHandler {

    /**
     * Get a handler for the given request.
     *
     * In addition to retrieving a handler for the request, the request itself may be modified - typically it will be
     * tagged with routing information.  It is also acceptable to simply return the request as is.  Play will switch to
     * using the returned request from this point in in its request handling.
     *
     * The reason why the API allows returning a modified request, rather than just wrapping the Handler in a new Handler
     * that modifies the request, is so that Play can pass this request to other handlers, such as error handlers, or
     * filters, and they will get the tagged/modified request.
     *
     * @param request The request to handle
     * @return The possibly modified/tagged request, and a handler to handle it
     */
    HandlerForRequest handlerForRequest(RequestHeader request);

    /**
     * Call to create the root Action of a request for a Java application.
     *
     * The request and actionMethod values are passed for information.  Implementations of this method should create
     * an instance of Action that invokes the injected action delegate.
     *
     * @deprecated Use ActionCreator instead.
     *
     * @param request The HTTP Request
     * @param actionMethod The action method containing the user code for this Action.
     * @return The default implementation returns a raw Action calling the method.
     */
    @Deprecated
    default Action createAction(Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx) {
                return delegate.call(ctx);
            }
        };
    }

    /**
     * Call to wrap the outer action of a Java application.
     *
     * This method is passed a fully composed action, allowing a last final global interceptor to be added to the
     * action if required.
     *
     * @deprecated Use ActionCreator instead.
     *
     * @param action The action to wrap.
     * @return A wrapped action.
     */
    @Deprecated
    default Action wrapAction(Action action) {
        return action;
    }

    /**
     * Adapt this to a Scala HttpRequestHandler
     */
    default play.api.http.HttpRequestHandler asScala() {
        return new JavaHttpRequestHandlerAdapter(this);
    }
}
