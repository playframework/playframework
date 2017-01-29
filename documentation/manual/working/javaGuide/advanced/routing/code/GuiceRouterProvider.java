/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
import play.routing.Router;
import router.RoutingDslBuilder;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

//#load-guice2
@Singleton
public class GuiceRouterProvider implements Provider<play.api.routing.Router> {

    private final Router javaRouter;

    @Inject
    public GuiceRouterProvider(RoutingDslBuilder router) {
        javaRouter = router.getRouter();
    }

    @Override
    public play.api.routing.Router get() {
        return javaRouter.asScala();
    }

}
//#load-guice2