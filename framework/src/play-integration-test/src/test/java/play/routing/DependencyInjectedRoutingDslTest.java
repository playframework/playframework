/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

public class DependencyInjectedRoutingDslTest extends AbstractRoutingDslTest {

    private static Application app;

    @BeforeClass
    public static void startApp() {
        app = new GuiceApplicationBuilder()
                .configure("play.allowGlobalApplication", true)
                .build();
        Helpers.start(app);
    }

    @Override
    Application application() {
        return app;
    }

    @Override
    RoutingDsl routingDsl() {
        return app.injector().instanceOf(RoutingDsl.class);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }
}
