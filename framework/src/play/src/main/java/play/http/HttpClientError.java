/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class HttpClientError implements HttpError<Object> {

    private final Http.RequestHeader request;
    private final int statusCode;
    private final Object error;
    private final HttpEntity entity;

    public HttpClientError(Http.RequestHeader request, int statusCode, Object error, HttpEntity entity) {
        this.request = request;
        this.statusCode = statusCode;
        this.error = error;
        this.entity = entity;
    }

    public int statusCode() {
        return statusCode;
    };

    @Override
    public Object error() {
        return error;
    }

    @Override
    public HttpEntity entity() {
        return entity;
    }

    @Override
    public Http.RequestHeader request() {
        return request;
    }

    @Override
    public play.api.http.HttpClientError asScala() {
        return new play.api.http.HttpClientError(request._underlyingHeader(), statusCode, error, entity.asScala());
    }

    public Result asResult() {
        return Results.status(statusCode).sendEntity(entity);
    }

}
