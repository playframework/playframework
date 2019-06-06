/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.routing.RoutingDsl;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static play.mvc.Results.ok;

// #load-guice2
@Singleton
public class GuiceRouterProvider implements Provider<play.api.routing.Router> {

  private final RoutingDsl routingDsl;

  @Inject
  public GuiceRouterProvider(RoutingDsl routingDsl) {
    this.routingDsl = routingDsl;
  }

  @Override
  public play.api.routing.Router get() {
    return routingDsl.GET("/hello/:to").routeTo(to -> ok("Hello " + to)).build().asScala();
  }
}
// #load-guice2
