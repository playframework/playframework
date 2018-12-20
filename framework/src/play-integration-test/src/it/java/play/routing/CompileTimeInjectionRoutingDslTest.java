/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import org.junit.BeforeClass;
import play.Application;
import play.ApplicationLoader;
import play.filters.components.NoHttpFiltersComponents;

public class CompileTimeInjectionRoutingDslTest extends AbstractRoutingDslTest {

    private static TestComponents components;
    private static Application application;

    @BeforeClass
    public static void startApp() {
        play.ApplicationLoader.Context context = play.ApplicationLoader.create(play.Environment.simple());
        components = new TestComponents(context);
        application = components.application();
    }

    @Override
    RoutingDsl routingDsl() {
        return components.routingDsl();
    }

    @Override
    Application application() {
        return application;
    }

    private static class TestComponents extends RoutingDslComponentsFromContext implements NoHttpFiltersComponents {

        TestComponents(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            return routingDsl().build();
        }
    }
}
