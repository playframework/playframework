/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import play.mvc.Http;
import play.mvc.Results;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

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
    CompletionStage<Result> handle(Http.RequestHeader req, String msg);

    class DefaultCSRFErrorHandler extends Results implements CSRFErrorHandler {

        private final CSRF.CSRFHttpErrorHandler errorHandler;

        @Inject
        public DefaultCSRFErrorHandler(CSRF.CSRFHttpErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        @Override
        public CompletionStage<Result> handle(Http.RequestHeader requestHeader, String msg) {
            return FutureConverters.toJava(errorHandler.handle(requestHeader.asScala(), msg))
                    .thenApply(play.api.mvc.Result::asJava);
        }

    }
}
