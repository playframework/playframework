/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import java.util.List;
import java.util.Optional;

import akka.japi.JavaPartialFunction;
import play.api.mvc.Handler;
import play.api.routing.HandlerDef;
import play.api.routing.SimpleRouter$;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.RequestHeader;

/**
 * The Java Router API
 */
public interface Router {

    List<RouteDocumentation> documentation();

    Optional<Handler> route(RequestHeader request);

    Router withPrefix(String prefix);

    default Router orElse(Router router) {
        return this.asScala().orElse(router.asScala()).asJava();
    }

    default play.api.routing.Router asScala() {
        return SimpleRouter$.MODULE$.apply(new JavaPartialFunction<play.api.mvc.RequestHeader, Handler>() {
            @Override
            public Handler apply(play.api.mvc.RequestHeader req, boolean isCheck) throws Exception {
                Optional<Handler> handler = route(req.asJava());
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
