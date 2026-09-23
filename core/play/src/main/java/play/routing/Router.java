/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import java.util.List;
import java.util.Optional;
import play.api.mvc.Handler;
import play.api.routing.HandlerDef;
import play.api.routing.SimpleRouter$;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.RequestHeader;

/** The Java Router API */
public interface Router {

  List<RouteDocumentation> documentation();

  Optional<Handler> route(RequestHeader request);

  Router withPrefix(String prefix);

  default Router orElse(Router router) {
    return this.asScala().orElse(router.asScala()).asJava();
  }

  default play.api.routing.Router asScala() {
    scala.PartialFunction<play.api.mvc.RequestHeader, Handler> routes =
        new scala.PartialFunction<>() {
          @Override
          public boolean isDefinedAt(play.api.mvc.RequestHeader request) {
            return Router.this.route(request.asJava()).isPresent();
          }

          @Override
          public Handler apply(play.api.mvc.RequestHeader request) {
            return Router.this
                .route(request.asJava())
                .orElseThrow(() -> new scala.MatchError(request));
          }

          @Override
          @SuppressWarnings("unchecked")
          public <A1 extends play.api.mvc.RequestHeader, B1> B1 applyOrElse(
              A1 request, scala.Function1<A1, B1> defaultFunction) {
            Optional<Handler> handler = Router.this.route(request.asJava());
            return handler.isPresent() ? (B1) handler.get() : defaultFunction.apply(request);
          }
        };
    return SimpleRouter$.MODULE$.apply(routes);
  }

  static Router empty() {
    return play.api.routing.Router$.MODULE$.empty().asJava();
  }

  /** Request attributes used by the router. */
  class Attrs {
    /** Key for the {@link HandlerDef} used to handle the request. */
    public static final TypedKey<HandlerDef> HANDLER_DEF =
        new TypedKey<>(play.api.routing.Router.Attrs$.MODULE$.HandlerDef());

    /** Key for the route parameters passed to the action method, in declaration order. */
    public static final TypedKey<scala.collection.immutable.SeqMap<String, Object>> ROUTE_PARAMS =
        new TypedKey<>(play.api.routing.Router.Attrs$.MODULE$.RouteParams());
  }

  class RouteDocumentation {
    private final String httpMethod;
    private final String pathPattern;
    private final String controllerMethodInvocation;

    public RouteDocumentation(
        String httpMethod, String pathPattern, String controllerMethodInvocation) {
      this.httpMethod = httpMethod;
      this.pathPattern = pathPattern;
      this.controllerMethodInvocation = controllerMethodInvocation;
    }

    public String getHttpMethod() {
      return httpMethod;
    }

    public String getPathPattern() {
      return pathPattern;
    }

    public String getControllerMethodInvocation() {
      return controllerMethodInvocation;
    }
  }
}
