/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.http.HttpErrorHandlerExceptions;
import play.api.routing.Router;
import play.mvc.Http.*;
import play.mvc.Result;
import play.mvc.Results;
import scala.Option;
import scala.Some;

import javax.inject.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Default implementation of the http error handler.
 *
 * This class is intended to be extended to allow reusing Play's default error handling functionality.
 */
public class DefaultHttpErrorHandler implements HttpErrorHandler {

    private final Option<String> playEditor;
    private final Environment environment;
    private final OptionalSourceMapper sourceMapper;
    private final Provider<Router> routes;

    @Inject
    public DefaultHttpErrorHandler(Configuration configuration, Environment environment,
                                   OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        this.environment = environment;
        this.sourceMapper = sourceMapper;
        this.routes = routes;

        this.playEditor = Option.apply(configuration.getString("play.editor"));
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series.
     *
     * @param request The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message The error message.
     */
    @Override
    public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
        if (statusCode == 400) {
            return onBadRequest(request, message);
        } else if (statusCode == 403) {
            return onForbidden(request, message);
        } else if (statusCode == 404) {
            return onNotFound(request, message);
        } else if (statusCode >= 400 && statusCode < 500) {
            return onOtherClientError(request, statusCode, message);
        } else {
            throw new IllegalArgumentException("onClientError invoked with non client error status code " + statusCode + ": " + message);
        }
    }

    /**
     * Invoked when a client makes a bad request.
     *
     * @param request The request that was bad.
     * @param message The error message.
     */
    protected CompletionStage<Result> onBadRequest(RequestHeader request, String message) {
        return CompletableFuture.completedFuture(Results.badRequest(views.html.defaultpages.badRequest.render(
                request.method(), request.uri(), message
        )));
    }

    /**
     * Invoked when a client makes a request that was forbidden.
     *
     * @param request The forbidden request.
     * @param message The error message.
     */
    protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
        return CompletableFuture.completedFuture(Results.forbidden(views.html.defaultpages.unauthorized.render()));
    }

    /**
     * Invoked when a handler or resource is not found.
     *
     * @param request The request that no handler was found to handle.
     * @param message A message.
     */
    protected CompletionStage<Result> onNotFound(RequestHeader request, String message){
        if (environment.isProd()) {
            return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.notFound.render(
                    request.method(), request.uri())));
        } else {
            return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.devNotFound.render(
                    request.method(), request.uri(), Some.apply(routes.get())
            )));
        }
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled 
     * by any of the other methods in this class already.
     *
     * @param request The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message The error message.
     */
    protected CompletionStage<Result> onOtherClientError(RequestHeader request, int statusCode, String message) {
        return CompletableFuture.completedFuture(Results.status(statusCode, views.html.defaultpages.badRequest.render(
                request.method(), request.uri(), message
        )));
    }

    /**
     * Invoked when a server error occurs.
     *
     * By default, the implementation of this method delegates to [[onProdServerError()]] when in prod mode, and
     * [[onDevServerError()]] in dev mode.  It is recommended, if you want Play's debug info on the error page in dev
     * mode, that you override [[onProdServerError()]] instead of this method.
     *
     * @param request The request that triggered the server error.
     * @param exception The server error.
     */
    @Override
    public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
        try {
            UsefulException usefulException = throwableToUsefulException(exception);

            logServerError(request, usefulException);

            switch (environment.mode()) {
                case PROD:
                    return onProdServerError(request, usefulException);
                default:
                    return onDevServerError(request, usefulException);
            }
        } catch (Exception e) {
            Logger.error("Error while handling error", e);
            return CompletableFuture.completedFuture(Results.internalServerError());
        }
    }

    /**
     * Responsible for logging server errors.
     *
     * This can be overridden to add additional logging information, eg. the id of the authenticated user.
     *
     * @param request The request that triggered the server error.
     * @param usefulException The server error.
     */
    protected void logServerError(RequestHeader request, UsefulException usefulException) {
        Logger.error(String.format("\n\n! @%s - Internal server error, for (%s) [%s] ->\n",
                        usefulException.id, request.method(), request.uri()),
                usefulException
        );
    }

    /**
     * Convert the given exception to an exception that Play can report more information about.
     *
     * This will generate an id for the exception, and in dev mode, will load the source code for the code that threw the
     * exception, making it possible to report on the location that the exception was thrown from.
     */
    private UsefulException throwableToUsefulException(final Throwable throwable) {
        return HttpErrorHandlerExceptions.throwableToUsefulException(sourceMapper.sourceMapper(), environment.isProd(), throwable);
    }

    /**
     * Invoked in dev mode when a server error occurs.
     *
     * @param request The request that triggered the error.
     * @param exception The exception.
     */
    protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
        return CompletableFuture.completedFuture(Results.internalServerError(views.html.defaultpages.devError.render(playEditor, exception)));
    }

    /**
     * Invoked in prod mode when a server error occurs.
     *
     * Override this rather than [[onServerError()]] if you don't want to change Play's debug output when logging errors
     * in dev mode.
     *
     * @param request The request that triggered the error.
     * @param exception The exception.
     */
    protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        return CompletableFuture.completedFuture(Results.internalServerError(views.html.defaultpages.error.render(exception)));
    }

}
