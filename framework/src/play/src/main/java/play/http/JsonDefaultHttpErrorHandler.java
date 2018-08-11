/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.libs.Json;
import play.libs.exception.ExceptionUtils;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * An alternative default HTTP error handler which will render errors as JSON messages instead of HTML pages.
 *
 * In Dev mode, exceptions thrown by the server code will be rendered in JSON messages.
 * In Prod mode, they will not be rendered.
 *
 * You could override how exceptions are rendered in Dev mode by extending this class and overriding
 * the [[formatDevServerErrorException]] method.
 */
public class JsonDefaultHttpErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    public JsonDefaultHttpErrorHandler(Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(config, environment, sourceMapper, routes);
    }

    /**
     * Invoked when a client makes a bad request.
     *
     * @param request The request that was bad.
     * @param message The error message.
     */
    @Override
    protected CompletionStage<Result> onBadRequest(RequestHeader request, String message) {
        ObjectNode result = Json.newObject();
        result.put("requestId", request.asScala().id());
        result.put("message", message);

        return CompletableFuture.completedFuture(Results.badRequest(error(result)));
    }

    /**
     * Invoked when a client makes a request that was forbidden.
     *
     * @param request The forbidden request.
     * @param message The error message.
     */
    @Override
    protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
        ObjectNode result = Json.newObject();
        result.put("requestId", request.asScala().id());
        result.put("message", message);

        return CompletableFuture.completedFuture(Results.forbidden(error(result)));
    }

    /**
     * Invoked when a handler or resource is not found.
     *
     * @param request The request that no handler was found to handle.
     * @param message A message.
     */
    @Override
    protected CompletionStage<Result> onNotFound(RequestHeader request, String message) {
        ObjectNode result = Json.newObject();
        result.put("requestId", request.asScala().id());
        result.put("message", message);

        return CompletableFuture.completedFuture(Results.notFound(error(result)));
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled by any of
     * the other methods in this class already.
     *
     * @param request The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message The error message.
     */
    @Override
    protected CompletionStage<Result> onOtherClientError(RequestHeader request, int statusCode, String message) {
        ObjectNode result = Json.newObject();
        result.put("requestId", request.asScala().id());
        result.put("message", message);

        return CompletableFuture.completedFuture(Results.status(statusCode, error(result)));
    }

    /**
     * Invoked in dev mode when a server error occurs.
     *
     * @param request The request that triggered the error.
     * @param exception The exception.
     */
    @Override
    protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
        ObjectNode exceptionJson = Json.newObject();
        exceptionJson.put("title", exception.title);
        exceptionJson.put("description", exception.description);
        exceptionJson.set("stacktrace", formatDevServerErrorException(exception.cause));

        ObjectNode result = Json.newObject();
        result.put("id", exception.id);
        result.put("requestId", request.asScala().id());
        result.set("exception", exceptionJson);

        return CompletableFuture.completedFuture(Results.internalServerError(error(result)));
    }

    /**
     * Format a [[Throwable]] as a JSON value.
     *
     * Override this method if you want to change how exceptions are rendered in Dev mode.
     *
     * @param exception an exception
     * @return a JSON representation of the passed expcetion
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
     * Override this rather than [[onServerError]] if you don't want to change Play's debug output when logging errors
     * in dev mode.
     *
     * @param request The request that triggered the error.
     * @param exception The exception.
     */
    @Override
    protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        ObjectNode result = Json.newObject();
        result.put("id", exception.id);

        return CompletableFuture.completedFuture(Results.internalServerError(error(result)));
    }

    private JsonNode error(JsonNode content) {
        ObjectNode result = Json.newObject();
        result.set("error", content);
        return result;
    }

}
