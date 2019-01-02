/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.http.HttpErrorHandlerExceptions;
import play.api.routing.Router;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import scala.Option;
import scala.Some;

/**
 * Default implementation of the http error handler.
 * <p>
 * This class is intended to be extended to allow reusing Play's default error handling functionality.
 *
 * The "play.editor" configuration setting is used here to give a link back to the source code when set
 * and development mode is on.
 */
public class DefaultHttpErrorHandler implements HttpErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpErrorHandler.class);

    private final Option<String> playEditor;
    private final Environment environment;
    private final OptionalSourceMapper sourceMapper;
    private final Provider<Router> routes;

    @Inject
    public DefaultHttpErrorHandler(
            Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        this.environment = environment;
        this.sourceMapper = sourceMapper;
        this.routes = routes;

        this.playEditor = Option.apply(config.hasPath("play.editor") ? config.getString("play.editor") : null);
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series.
     *
     * The base implementation calls onBadRequest, onForbidden, onNotFound, or onOtherClientError
     * depending on the HTTP status code.
     *
     * @param request    The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message    The error message.
     * @return a CompletionStage containing the Result.
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
     * <p>
     * Returns Results.badRequest (400) with the included template from {@code views.html.defaultpages.badRequest} as the content.
     *
     * @param request The request that was bad.
     * @param message The error message.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onBadRequest(RequestHeader request, String message) {
        return CompletableFuture.completedFuture(Results.badRequest(views.html.defaultpages.badRequest.render(
                request.method(), request.uri(), message, request.asScala()
        )));
    }

    /**
     * Invoked when a client makes a request that was forbidden.
     * <p>
     * Returns Results.forbidden (401) with the included template from {@code views.html.defaultpages.unauthorized} as the content.
     *
     * @param request The forbidden request.
     * @param message The error message.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
        return CompletableFuture.completedFuture(Results.forbidden(views.html.defaultpages.unauthorized.render(request.asScala())));
    }

    /**
     * Invoked when a handler or resource is not found.
     * <p>
     * If the environment's mode is production, then returns Results.notFound (404) with the included template from `views.html.defaultpages.notFound` as the content.
     * <p>
     * Otherwise, Results.notFound (404) is rendered with {@code views.html.defaultpages.devNotFound} template.
     *
     * @param request The request that no handler was found to handle.
     * @param message A message, which is not used by the default implementation.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onNotFound(RequestHeader request, String message) {
        if (environment.isProd()) {
            return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.notFound.render(
                    request.method(), request.uri(), request.asScala())));
        } else {
            return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.devNotFound.render(
                    request.method(), request.uri(), Some.apply(routes.get()), request.asScala()
            )));
        }
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled
     * by any of the other methods in this class already.
     *
     * The base implementation uses {@code views.html.defaultpages.badRequest} template with the given status.
     *
     * @param request    The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message    The error message.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onOtherClientError(RequestHeader request, int statusCode, String message) {
        return CompletableFuture.completedFuture(Results.status(statusCode, views.html.defaultpages.badRequest.render(
                request.method(), request.uri(), message, request.asScala()
        )));
    }

    /**
     * Invoked when a server error occurs.
     * <p>
     * By default, the implementation of this method delegates to [[onProdServerError()]] when in prod mode, and
     * [[onDevServerError()]] in dev mode.  It is recommended, if you want Play's debug info on the error page in dev
     * mode, that you override [[onProdServerError()]] instead of this method.
     *
     * @param request   The request that triggered the server error.
     * @param exception The server error.
     * @return a CompletionStage containing the Result.
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
            logger.error("Error while handling error", e);
            return CompletableFuture.completedFuture(Results.internalServerError());
        }
    }

    /**
     * Responsible for logging server errors.
     * <p>
     * The base implementation uses a SLF4J Logger.  If a special annotation is desired for internal server errors, you may want to use SLF4J directly with the Marker API to distinguish server errors from application errors.
     * <p>
     * This can also be overridden to add additional logging information, eg. the id of the authenticated user.
     *
     * @param request         The request that triggered the server error.
     * @param usefulException The server error.
     */
    protected void logServerError(RequestHeader request, UsefulException usefulException) {
        logger.error(String.format("\n\n! @%s - Internal server error, for (%s) [%s] ->\n",
                usefulException.id, request.method(), request.uri()),
                usefulException
        );
    }

    /**
     * Convert the given exception to an exception that Play can report more information about.
     * <p>
     * This will generate an id for the exception, and in dev mode, will load the source code for the code that threw the
     * exception, making it possible to report on the location that the exception was thrown from.
     */
    protected final UsefulException throwableToUsefulException(final Throwable throwable) {
        return HttpErrorHandlerExceptions.throwableToUsefulException(sourceMapper.sourceMapper(), environment.isProd(), throwable);
    }

    /**
     * Invoked in dev mode when a server error occurs.  Note that this method is where the URL set by play.editor is used.
     * <p>
     * The base implementation returns {@code Results.internalServerError} with the content of {@code views.html.defaultpages.devError}.
     *
     * @param request   The request that triggered the error.
     * @param exception The exception.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
        return CompletableFuture.completedFuture(Results.internalServerError(views.html.defaultpages.devError.render(playEditor, exception, request.asScala())));
    }

    /**
     * Invoked in prod mode when a server error occurs.
     * <p>
     * The base implementation returns {@code Results.internalServerError} with the content of {@code views.html.defaultpages.error} template.
     * </p>
     * <p>
     * Override this rather than [[onServerError()]] if you don't want to change Play's debug output when logging errors
     * in dev mode.
     *
     * @param request   The request that triggered the error.
     * @param exception The exception.
     * @return a CompletionStage containing the Result.
     */
    protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        return CompletableFuture.completedFuture(Results.internalServerError(views.html.defaultpages.error.render(exception, request.asScala())));
    }

}
