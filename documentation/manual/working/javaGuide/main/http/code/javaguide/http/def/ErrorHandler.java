/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http.def;

//#default
import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.F.*;
import play.mvc.Http.*;
import play.mvc.*;

import javax.inject.*;

public class ErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    public ErrorHandler(Configuration configuration, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
    }

    protected Promise<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        return Promise.<Result>pure(
                Results.internalServerError("A server error occurred: " + exception.getMessage())
        );
    }

    protected Promise<Result> onForbidden(RequestHeader request, String message) {
        return Promise.<Result>pure(
                Results.forbidden("You're not allowed to access this resource.")
        );
    }
}
//#default
