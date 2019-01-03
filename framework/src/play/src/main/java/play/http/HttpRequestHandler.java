/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import play.core.j.JavaHttpRequestHandlerAdapter;
import play.mvc.Http.RequestHeader;

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
     * @return a Scala HttpRequestHandler
     */
    default play.api.http.HttpRequestHandler asScala() {
        return new JavaHttpRequestHandlerAdapter(this);
    }
}
