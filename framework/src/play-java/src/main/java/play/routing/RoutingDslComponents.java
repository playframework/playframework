/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import play.components.BodyParserComponents;

/**
 * Java Components for RoutingDsl.
 *
 * <p>Usage:</p>
 * <pre>
 * public class MyComponentsWithRouter extends RoutingDslComponentsFromContext implements HttpFiltersComponents {
 *
 *     public MyComponentsWithRouter(ApplicationLoader.Context context) {
 *         super(context);
 *     }
 *
 *     public Router router() {
 *         // routingDsl method is provided by RoutingDslComponentsFromContext
 *         return routingDsl()
 *              .GET("/path").routeTo(() -&gt; Results.ok("The content"))
 *              .build();
 *     }
 *
 *     // other methods
 * }
 * </pre>
 *
 * @see RoutingDsl
 */
public interface RoutingDslComponents extends BodyParserComponents {

    default RoutingDsl routingDsl() {
        return new RoutingDsl(defaultBodyParser(), javaContextComponents());
    }

}
