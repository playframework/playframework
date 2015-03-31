/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf;

import play.libs.F;
import play.mvc.Http;
import play.mvc.Results;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * This interface handles the CSRF error.
 */
public interface CSRFErrorHandler {

    /**
     * Handle the CSRF error.
     *
     * @param req The request
     * @param msg message is passed by framework.
     * @return Client gets this result.
     */
    F.Promise<Result> handle(Http.RequestHeader req, String msg);

    public static class DefaultCSRFErrorHandler extends Results implements CSRFErrorHandler {

        private final CSRF.CSRFHttpErrorHandler errorHandler;

        @Inject
        public DefaultCSRFErrorHandler(CSRF.CSRFHttpErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        @Override
        public F.Promise<Result> handle(Http.RequestHeader req, String msg) {
            return F.Promise.wrap(errorHandler.handle(req._underlyingHeader(), msg)).map(Status::new);
        }

    }
}
