/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Provider;

import play.mvc.Action;
import play.mvc.Http.Request;

/**
 * An action creator that delegates to HttpRequestHandler.
 *
 * If you wish to customize these behaviors, you should override the ones here rather than the (deprecated) ones in
 * HttpRequestHandler.
 */
public class HttpRequestHandlerActionCreator implements ActionCreator {

  private final Provider<HttpRequestHandler> httpRequestHandler;

  @Inject
  public HttpRequestHandlerActionCreator(Provider<HttpRequestHandler> httpRequestHandler) {
    this.httpRequestHandler = httpRequestHandler;
  }

  @Override
  public Action createAction(Request request, Method actionMethod) {
    return httpRequestHandler.get().createAction(request, actionMethod);
  }

  @Override
  public Action wrapAction(Action action) {
    return httpRequestHandler.get().wrapAction(action);
  }
}
