/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

public class DependencyInjectedRoutingDslTest extends AbstractRoutingDslTest {

    @Override
    RoutingDsl routingDsl() {
        return app.injector().instanceOf(RoutingDsl.class);
    }

    @Override
    Router router(RoutingDsl routing) {
        return routing.build();
    }
}
