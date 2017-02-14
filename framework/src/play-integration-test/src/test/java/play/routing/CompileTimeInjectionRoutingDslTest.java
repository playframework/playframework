/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import org.junit.BeforeClass;
import play.api.ApplicationLoader;
import play.api.Mode;

import java.io.File;

public class CompileTimeInjectionRoutingDslTest extends AbstractRoutingDslTest {

    private static TestComponents components;

    @BeforeClass
    public static void startApp() {
        play.api.Environment environment = play.api.Environment.simple(new File("."), Mode.Test());
        play.api.ApplicationLoader.Context context = new play.api.ApplicationLoader.Context(
                environment,
                scala.Option.empty(),
                new play.core.DefaultWebCommands(),
                play.api.Configuration.load(environment),
                new play.api.inject.DefaultApplicationLifecycle()
        );
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
