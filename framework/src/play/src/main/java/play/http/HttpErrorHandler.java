/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.http;

import play.libs.F;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

/**
 * Component for handling HTTP errors in Play.
 *
 * @since 2.4.0
 */
public interface HttpErrorHandler {

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series.
     *
     * @param request The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message The error message.
     */
    F.Promise<Result> onClientError(RequestHeader request, int statusCode, String message);

    /**
     * Invoked when a server error occurs.
     *
     * @param request The request that triggered the server error.
     * @param exception The server error.
     */
    F.Promise<Result> onServerError(RequestHeader request, Throwable exception);
}
