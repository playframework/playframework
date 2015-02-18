/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.root;

//#root
import play.http.HttpErrorHandler;
import play.mvc.*;
import play.mvc.Http.*;
import play.libs.F.*;

public class ErrorHandler implements HttpErrorHandler {
    public Promise<Result> onClientError(RequestHeader request, int statusCode, String message) {
        return Promise.<Result>pure(
            Results.status(statusCode, "A client error occurred: " + message)
        );
    }

    public Promise<Result> onServerError(RequestHeader request, Throwable exception) {
        return Promise.<Result>pure(
            Results.internalServerError("A server error occurred: " + exception.getMessage())
        );
    }
}
//#root
