/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf;

import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import scala.jdk.javaapi.FutureConverters;

/** This interface handles the CSRF error. */
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
      return FutureConverters.asJava(errorHandler.handle(requestHeader.asScala(), msg))
          .thenApply(play.api.mvc.Result::asJava);
    }
  }
}
