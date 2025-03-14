/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import jakarta.inject.Inject;
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
    Tuple2<play.api.mvc.RequestHeader, Handler> result =
        underlying.handlerForRequest(request.asScala());
    return new HandlerForRequest(result._1().asJava(), result._2());
  }
}
