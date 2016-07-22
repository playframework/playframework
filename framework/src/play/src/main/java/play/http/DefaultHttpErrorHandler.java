/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;

import com.typesafe.config.Config;
import play.Configuration;
import play.Environment;
import play.Logger;
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
 *
 * This class is intended to be extended to allow reusing Play's default error handling functionality.
 */
public class DefaultHttpErrorHandler implements HttpErrorHandler {

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

    @Deprecated
    public DefaultHttpErrorHandler(
        Configuration config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        this(config.underlying(), environment, sourceMapper, routes);
    }

    /**
     * Invoked when a error occurs.
     *
     * @param error The error message.
     */
    @Override
    public CompletionStage<Result> onError(HttpError<?> error) {
        if (error instanceof HttpServerError) {
            return onServerError((HttpServerError)error);
        } else if (error instanceof HttpClientError) {
            return onClientError((HttpClientError)error);
        } else {
            throw new IllegalStateException("HttpError needs to be either of type HttpServerError or of type HttpClientError");
        }
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series.
     *
     * @param error The error.
     */
    public CompletionStage<Result> onClientError(HttpClientError error) {
        if (error.statusCode() == 400) {
            return onBadRequest(error);
        } else if (error.statusCode() == 403) {
            return onForbidden(error);
        } else if (error.statusCode() == 404) {
            return onNotFound(error);
        } else if (error.statusCode() >= 400 && error.statusCode() < 500) {
            return onOtherClientError(error);
        } else {
            throw new IllegalArgumentException("onClientError invoked with non client error status code " + error.statusCode() + ": " + error.error().toString());
        }
    }

    /**
     * Invoked when a client makes a bad request.
     *
     * @param error The error error.
     */
    protected CompletionStage<Result> onBadRequest(HttpClientError error) {
        if (error.error() instanceof String) {
            return CompletableFuture.completedFuture(Results.badRequest(views.html.defaultpages.badRequest.render(
                    error.request().method(), error.request().uri(), (String)error.error()
            )));
        } else {
            return CompletableFuture.completedFuture(error.asResult());
        }
    }

    /**
     * Invoked when a client makes a request that was forbidden.
     *
     * @param error The error.
     */
    protected CompletionStage<Result> onForbidden(HttpClientError error) {
        if (error.error() instanceof String) {
            return CompletableFuture.completedFuture(Results.forbidden(views.html.defaultpages.unauthorized.render()));
        } else {
            return CompletableFuture.completedFuture(error.asResult());
        }
    }

    /**
     * Invoked when a handler or resource is not found.
     *
     * @param error The error.
     */
    protected CompletionStage<Result> onNotFound(HttpClientError error){
        if (error.error() instanceof String) {
            if (environment.isProd()) {
                return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.notFound.render(
                        error.request().method(), error.request().uri())));
            } else {
                return CompletableFuture.completedFuture(Results.notFound(views.html.defaultpages.devNotFound.render(
                        error.request().method(), error.request().uri(), Some.apply(routes.get())
                )));
            }
        } else {
            return CompletableFuture.completedFuture(error.asResult());
        }
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled
     * by any of the other methods in this class already.
     *
     * @param error The error.
     */
    protected CompletionStage<Result> onOtherClientError(HttpClientError error) {
        if (error.error() instanceof String) {
            return CompletableFuture.completedFuture(Results.status(error.statusCode(), views.html.defaultpages.badRequest.render(
                    error.request().method(), error.request().uri(), (String)error.error()
            )));
        } else {
            return CompletableFuture.completedFuture(error.asResult());
        }
    }

    /**
     * Invoked when a server error occurs.
     *
     * By default, the implementation of this method delegates to [[onProdServerError()]] when in prod mode, and
     * [[onDevServerError()]] in dev mode.  It is recommended, if you want Play's debug info on the error page in dev
     * mode, that you override [[onProdServerError()]] instead of this method.
     *
     * @param exception The server error.
     */
    public CompletionStage<Result> onServerError(HttpServerError exception) {
        try {
            UsefulException usefulException = throwableToUsefulException(exception.error());

            logServerError(exception.request(), usefulException);

            switch (environment.mode()) {
                case PROD:
                    return onProdServerError(exception.request(), usefulException);
                default:
                    return onDevServerError(exception.request(), usefulException);
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
