/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import play.api.http.MediaRange;
import play.libs.Scala;
import play.mvc.Http;
import play.mvc.Result;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletionStage;

/**
 * An `HttpErrorHandler` that delegates to one of several `HttpErrorHandlers` depending on the client's media type
 * preference. The order of preference is defined by the client's `Accept` header. The handlers are specified as a
 * `LinkedHashMap`, and the ordering of the map determines the order in which media types are chosen when they are
 * equally preferred by a specific media range (e.g. `*\/*`).
 */
public class PreferredMediaTypeHttpErrorHandler implements HttpErrorHandler {
    private LinkedHashMap<String, HttpErrorHandler> errorHandlerMap;

    public PreferredMediaTypeHttpErrorHandler(LinkedHashMap<String, HttpErrorHandler> errorHandlerMap) {
        if (errorHandlerMap.isEmpty()) {
            throw new IllegalArgumentException("Map must not be empty!");
        }
        this.errorHandlerMap = new LinkedHashMap<>(errorHandlerMap);
    }

    protected HttpErrorHandler preferred(Http.RequestHeader request) {
        String preferredContentType = Scala.orNull(MediaRange.preferred(
                Scala.toSeq(request.acceptedTypes()),
                Scala.toSeq(errorHandlerMap.keySet().toArray(new String[]{}))
        ));
        if (preferredContentType == null) {
            return errorHandlerMap.values().iterator().next();
        } else {
            return errorHandlerMap.get(preferredContentType);
        }
    }

    @Override
    public CompletionStage<Result> onClientError(Http.RequestHeader request, int statusCode, String message) {
        return preferred(request).onClientError(request, statusCode, message);
    }

    @Override
    public CompletionStage<Result> onServerError(Http.RequestHeader request, Throwable exception) {
        return preferred(request).onServerError(request, exception);
    }
}
