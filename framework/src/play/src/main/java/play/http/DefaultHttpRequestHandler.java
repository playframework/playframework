/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import javax.inject.Inject;

import play.api.mvc.Handler;
import play.mvc.Http.RequestHeader;
import scala.Tuple2;

public class DefaultHttpRequestHandler implements HttpRequestHandler {

    private final play.api.http.JavaCompatibleHttpRequestHandler underlying;

    @Inject
    public DefaultHttpRequestHandler(play.api.http.JavaCompatibleHttpRequestHandler underlying) {
        this.underlying = underlying;
    }

    @Override
    public HandlerForRequest handlerForRequest(RequestHeader request) {
        Tuple2<play.api.mvc.RequestHeader, Handler> result = underlying.handlerForRequest(request.asScala());
        return new HandlerForRequest(result._1().asJava(), result._2());
    }
}
