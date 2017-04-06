/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import play.components.BodyParserComponents;

/**
 * Java Components for RoutingDsl.
 *
 * @see RoutingDsl
 */
public interface RoutingDslComponents extends BodyParserComponents {

    default RoutingDsl routingDsl() {
        return new RoutingDsl(defaultScalaParser(), javaContextComponents());
    }

}
