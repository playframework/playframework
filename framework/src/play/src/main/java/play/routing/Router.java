/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import java.util.List;
import java.util.Optional;

import akka.japi.JavaPartialFunction;
import play.api.mvc.Handler;
import play.api.routing.HandlerDef;
import play.api.routing.SimpleRouter$;
import play.core.j.RequestHeaderImpl;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.RequestHeader;

/**
 * The Java Router API
 */
public interface Router {

    List<RouteDocumentation> documentation();

    Optional<Handler> route(RequestHeader request);

    Router withPrefix(String prefix);

    default play.api.routing.Router asScala() {
        return SimpleRouter$.MODULE$.apply(new JavaPartialFunction<play.api.mvc.RequestHeader, Handler>() {
            @Override
            public Handler apply(play.api.mvc.RequestHeader req, boolean isCheck) throws Exception {
                Optional<Handler> handler = route(new RequestHeaderImpl(req));
                if (handler.isPresent()) {
                    return handler.get();
                } else if (isCheck) {
                    return null;
                } else {
                    throw noMatch();
                }
            }
        });
    }

    static Router empty() {
        return play.api.routing.Router$.MODULE$.empty().asJava();
    }

    /**
     * Request attributes used by the router.
     */
    class Attrs {
        /**
         * Key for the {@link HandlerDef} used to handle the request.
         */
        public static final TypedKey<HandlerDef> HANDLER_DEF = new TypedKey<>(play.api.routing.Router.Attrs$.MODULE$.HandlerDef());
    }

    // These should match those in play.api.routing.Router.Tags
    class Tags {
        /**
         * The verb that the router matched
         * @deprecated Use <code>Router.Attrs.HandlerDef</code> instead.
         */
        @Deprecated
        public static final String ROUTE_VERB = "ROUTE_VERB";
        /**
         * The pattern that the router used to match the path
         * @deprecated Use <code>Router.Attrs.HandlerDef</code> instead.
         */
        @Deprecated
        public static final String ROUTE_PATTERN = "ROUTE_PATTERN";
        /**
         * The controller that was routed to
         * @deprecated Use <code>Router.Attrs.HandlerDef</code> instead.
         */
        @Deprecated
        public static final String ROUTE_CONTROLLER = "ROUTE_CONTROLLER";
        /**
         * The method on the controller that was invoked
         * @deprecated Use <code>Router.Attrs.HandlerDef</code> instead.
         */
        @Deprecated
        public static final String ROUTE_ACTION_METHOD = "ROUTE_ACTION_METHOD";
        /**
         * The comments in the routes file that were above the route
         * @deprecated Use <code>Router.Attrs.HandlerDef</code> instead.
         */
        @Deprecated
        public static final String ROUTE_COMMENTS = "ROUTE_COMMENTS";
    }

    class RouteDocumentation {
        private final String httpMethod;
        private final String pathPattern;
        private final String controllerMethodInvocation;

        public RouteDocumentation(String httpMethod, String pathPattern, String controllerMethodInvocation) {
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
