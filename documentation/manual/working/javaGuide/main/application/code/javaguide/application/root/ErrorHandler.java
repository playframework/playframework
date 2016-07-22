/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.application.root;

//#root
import play.http.HttpClientError;
import play.http.HttpError;
import play.http.HttpErrorHandler;
import play.http.HttpServerError;
import play.mvc.*;
import play.mvc.Http.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Singleton;

@Singleton
public class ErrorHandler implements HttpErrorHandler {

    @Override
    public CompletionStage<Result> onError(HttpError<?> error) {
        if (error instanceof HttpClientError) {
            HttpClientError clientError = (HttpClientError)error;
            if (clientError.error() instanceof String) {
                return CompletableFuture.completedFuture(
                        Results.status(clientError.statusCode(), "A client error occurred: " + clientError.error())
                );
            } else {
                return CompletableFuture.completedFuture(clientError.asResult());
            }
        } else {
            return CompletableFuture.completedFuture(
                    error.asResult()
            );
        }
    }
}
//#root
