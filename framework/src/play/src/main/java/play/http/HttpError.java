/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.mvc.Http;
import play.mvc.Result;

import java.nio.charset.StandardCharsets;

public interface HttpError<T> {

    T error();

    HttpEntity entity();

    Http.RequestHeader request();

    play.api.http.HttpError<T> asScala();

    Result asResult();

    static HttpClientError fromString(Http.RequestHeader request, int statusCode, String message) {
        HttpEntity entity = HttpEntity.fromString(message, StandardCharsets.UTF_8.name());
        return new HttpClientError(request, statusCode, message, entity);
    };

}
