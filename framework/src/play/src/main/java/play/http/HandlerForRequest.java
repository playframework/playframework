/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.api.mvc.Handler;
import play.mvc.Http.RequestHeader;

/**
 * A request and a handler to handle it.
 */
public class HandlerForRequest {
  private final RequestHeader request;
  private final Handler handler;

  public HandlerForRequest(RequestHeader request, Handler handler) {
    this.request = request;
    this.handler = handler;
  }

  public RequestHeader getRequest() {
    return request;
  }

  public Handler getHandler() {
    return handler;
  }
}
