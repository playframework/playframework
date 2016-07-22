/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class HttpServerError implements HttpError<Throwable> {

    private final Http.RequestHeader request;
    private final Throwable error;

    public HttpServerError(Http.RequestHeader request, Throwable error) {
        this.request = request;
        this.error = error;
    }

    @Override
    public Throwable error() {
        return error;
    }

    @Override
    public HttpEntity entity() {
        return asScala().entity().asJava();
    }

    @Override
    public Http.RequestHeader request() {
        return request;
    }

    @Override
    public play.api.http.HttpError<Throwable> asScala() {
        return play.api.http.HttpServerError.apply(request._underlyingHeader(), error);
    }

    public Result asResult() {
        return Results.internalServerError().sendEntity(entity());
    }

}
