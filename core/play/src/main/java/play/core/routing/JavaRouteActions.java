/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing;

import java.util.concurrent.CompletionStage;
import play.api.mvc.Handler;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;

/**
 * The controller method calls of a router that the routes compiler generates as Java source code.
 *
 * <p>Each interface accepts controller methods with another return type. The Java compiler picks
 * the interface that fits the controller method of a route, and rejects a controller method whose
 * return type Play can't handle. The parameters are the bound parameters of the route, in the order
 * of the routes file, without fixed parameters and without the request.
 */
public final class JavaRouteActions {

  private JavaRouteActions() {}

  /** Calls a controller method that returns a result. */
  @FunctionalInterface
  public interface ResultAction {
    Result call(Object[] params);
  }

  /** Calls a controller method that returns a promise of a result. */
  @FunctionalInterface
  public interface AsyncResultAction {
    CompletionStage<Result> call(Object[] params);
  }

  /** Calls a controller method that takes the request and returns a result. */
  @FunctionalInterface
  public interface RequestResultAction {
    Result call(Http.Request request, Object[] params);
  }

  /** Calls a controller method that takes the request and returns a promise of a result. */
  @FunctionalInterface
  public interface AsyncRequestResultAction {
    CompletionStage<Result> call(Http.Request request, Object[] params);
  }

  /** Calls a controller method that returns a Java WebSocket. */
  @FunctionalInterface
  public interface WebSocketAction {
    WebSocket call(Object[] params);
  }

  /** Calls a controller method that returns a handler, such as a Scala action. */
  @FunctionalInterface
  public interface HandlerAction {
    Handler call(Object[] params);
  }
}
