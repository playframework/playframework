/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import org.junit.BeforeClass;
import play.api.ApplicationLoader;

public class CompileTimeInjectionRoutingDslTest extends AbstractRoutingDslTest {

    private static TestComponents components;

    @BeforeClass
    public static void startApp() {
        play.api.ApplicationLoader.Context context = play.ApplicationLoader.create(play.Environment.simple()).underlying();
        components = new TestComponents(context);
    }

    @Override
    RoutingDsl routingDsl() {
        return components.routingDsl();
    }

    private static class TestComponents extends RoutingDslComponentsFromContext {

        TestComponents(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public play.api.routing.Router router() {
            return routingDsl().build().asScala();
        }
    }
}
