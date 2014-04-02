/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf;

import play.mvc.Results;
import play.mvc.Result;

/**
 * This interface handles the CSRF error.
 */
public interface CSRFErrorHandler {

    /**
     * Handle the CSRF error.
     *
     * @param msg message is passed by framework.
     * @return Client gets this result.
     */
    Result handle(String msg);

    public static class DefaultCSRFErrorHandler extends Results implements CSRFErrorHandler {

        @Override
        public Result handle(String msg) {
            return (Result) forbidden(msg);
        }

    }
}
