/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import play.api.http.HttpErrorInfo;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

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
   * @param statusCode The error status code. Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   * @return a CompletionStage with the Result.
   */
  CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message);

  /**
   * Invoked when a server error occurs.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   * @return a CompletionStage with the Result.
   */
  CompletionStage<Result> onServerError(RequestHeader request, Throwable exception);

  /** Request attributes used by the error handler. */
  class Attrs {
    public static final TypedKey<HttpErrorInfo> HTTP_ERROR_INFO =
        new TypedKey<>(play.api.http.HttpErrorHandler.Attrs$.MODULE$.HttpErrorInfo());
  }
}
