/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.http.HttpErrorHandlerExceptions;
import play.libs.Json;
import play.libs.exception.ExceptionUtils;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

/**
 * An alternative default HTTP error handler which will render errors as JSON messages instead of
 * HTML pages.
 *
 * <p>In Dev mode, exceptions thrown by the server code will be rendered in JSON messages. In Prod
 * mode, they will not be rendered.
 *
 * <p>You could override how exceptions are rendered in Dev mode by extending this class and
 * overriding the [[formatDevServerErrorException]] method.
 */
public class JsonHttpErrorHandler implements HttpErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(JsonHttpErrorHandler.class);

  private final Environment environment;
  private final OptionalSourceMapper sourceMapper;

  @Inject
  public JsonHttpErrorHandler(Environment environment, OptionalSourceMapper sourceMapper) {
    this.environment = environment;
    this.sourceMapper = sourceMapper;
  }

  @Override
  public CompletionStage<Result> onClientError(
      RequestHeader request, int statusCode, String message) {
    if (!play.api.http.Status$.MODULE$.isClientError(statusCode)) {
      throw new IllegalArgumentException(
          "onClientError invoked with non client error status code " + statusCode + ": " + message);
    }

    ObjectNode result = Json.newObject();
    result.put("requestId", request.asScala().id());
    result.put("title", message);
    result.put("status", statusCode);

    return CompletableFuture.completedFuture(Results.status(statusCode, result));
  }

  @Override
  public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
    try {
      UsefulException usefulException = throwableToUsefulException(exception);

      logServerError(request, usefulException);

      switch (environment.mode()) {
        case PROD:
          return CompletableFuture.completedFuture(
              Results.internalServerError(prodServerError(request, usefulException)));
        default:
          return CompletableFuture.completedFuture(
              Results.internalServerError(devServerError(request, usefulException)));
      }
    } catch (Exception e) {
      logger.error("Error while handling error", e);
      return CompletableFuture.completedFuture(
          Results.internalServerError(fatalErrorJson(request, e)));
    }
  }

  /**
   * Invoked when handling a server error with this error handler failed.
   *
   * <p>As a last resort this method allows you to return a (simple) error message that will be send
   * along with a "500 Internal Server Error" response. It's highly recommended to just return a
   * simple JsonNode, without doing any fancy processing inside the method (like accessing
   * files,...) that could throw exceptions. This is your last chance to send a meaningful error
   * message when everything else failed.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   * @return An error JSON which will be send as last resort in case handling a server error with
   *     this error handler failed.
   */
  protected JsonNode fatalErrorJson(RequestHeader request, Throwable exception) {
    return Json.newObject();
  }

  /**
   * Convert the given exception to an exception that Play can report more information about.
   *
   * <p>This will generate an id for the exception, and in dev mode, will load the source code for
   * the code that threw the exception, making it possible to report on the location that the
   * exception was thrown from.
   */
  protected final UsefulException throwableToUsefulException(final Throwable throwable) {
    return HttpErrorHandlerExceptions.throwableToUsefulException(
        sourceMapper.sourceMapper(), environment.isProd(), throwable);
  }

  /**
   * Responsible for logging server errors.
   *
   * <p>The base implementation uses a SLF4J logger. If a special annotation is desired for internal
   * server errors, you may want to use SLF4J directly with the Marker API to distinguish server
   * errors from application errors.
   *
   * <p>This can also be overridden to add additional logging information, eg. the id of the
   * authenticated user.
   *
   * @param request The request that triggered the server error.
   * @param usefulException The server error.
   */
  protected void logServerError(RequestHeader request, UsefulException usefulException) {
    logger.error(
        String.format(
            "\n\n! @%s - Internal server error, for (%s) [%s] ->\n",
            usefulException.id, request.method(), request.uri()),
        usefulException);
  }

  /**
   * Invoked in dev mode when a server error occurs.
   *
   * @param request The request that triggered the error.
   * @param exception The exception.
   */
  protected JsonNode devServerError(RequestHeader request, UsefulException exception) {
    ObjectNode result = Json.newObject();
    result.put("id", exception.id);
    result.put("requestId", request.asScala().id());
    result.put("status", INTERNAL_SERVER_ERROR);
    result.put("title", exception.title);
    result.put("detail", exception.description);
    result.set("stacktrace", formatDevServerErrorException(exception.cause));

    return result;
  }

  /**
   * Format a {@link Throwable} as a JSON value.
   *
   * <p>Override this method if you want to change how exceptions are rendered in Dev mode.
   *
   * @param exception an exception
   * @return a JSON representation of the passed exception
   */
  protected JsonNode formatDevServerErrorException(Throwable exception) {
    ArrayNode res = Json.newArray();
    for (String s : ExceptionUtils.getStackFrames(exception)) {
      res.add(s.trim());
    }
    return res;
  }

  /**
   * Invoked in prod mode when a server error occurs.
   *
   * <p>Override this rather than {@link #onServerError(RequestHeader, Throwable)} if you don't want
   * to change Play's debug output when logging errors in dev mode.
   *
   * @param request The request that triggered the error.
   * @param exception The exception.
   */
  protected JsonNode prodServerError(RequestHeader request, UsefulException exception) {
    ObjectNode result = Json.newObject();
    result.put("id", exception.id);
    result.put("requestId", request.asScala().id());
    result.put("status", INTERNAL_SERVER_ERROR);
    result.put("title", "Internal server error");

    return result;
  }
}
