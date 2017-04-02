/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import org.junit.BeforeClass;
import play.Application;
import play.DefaultApplication;
import play.api.ApplicationLoader;
import play.api.http.HttpFilters;
import play.api.mvc.EssentialFilter;
import play.inject.DelegateInjector;
import play.inject.Injector;
import play.libs.Scala;
import scala.collection.Seq;

public class CompileTimeInjectionRoutingDslTest extends AbstractRoutingDslTest {

    private static TestComponents components;
    private static Application application;

    @BeforeClass
    public static void startApp() {
        play.api.ApplicationLoader.Context context = play.ApplicationLoader.create(play.Environment.simple()).asScala();
        components = new TestComponents(context);
        Injector injector = new DelegateInjector(components.injector());
        application = new DefaultApplication(components.application(), injector);
    }

    @Override
    RoutingDsl routingDsl() {
        return components.routingDsl();
    }

    @Override
    Application application() {
        return application;
    }

    private static class TestComponents extends RoutingDslComponentsFromContext {

        TestComponents(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public play.api.routing.Router router() {
            return routingDsl().build().asScala();
        }

        @Override
        public Seq<EssentialFilter> httpFilters() {
            return Scala.emptySeq();
        }
    }
}
