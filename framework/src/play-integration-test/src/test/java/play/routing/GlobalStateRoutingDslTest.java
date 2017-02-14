/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

/**
 * @deprecated as of 2.6.0.
 */
@Deprecated
public class GlobalStateRoutingDslTest extends AbstractRoutingDslTest {
    private static Application app;

    @BeforeClass
    public static void startApp() {
        app = new GuiceApplicationBuilder().build();
        Helpers.start(app);
    }

    @Override
    RoutingDsl routingDsl() {
        return new RoutingDsl();
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }
}
