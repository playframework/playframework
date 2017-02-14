/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

// Should be removed together with deprecated methods and constructor at RoutingDsl
public class NewRoutingDslTest extends AbstractRoutingDslTest {

    @Override
    RoutingDsl routingDsl() {
        return new RoutingDsl();
    }

    @Override
    Router router(RoutingDsl routing) {
        return routing.build();
    }
}
