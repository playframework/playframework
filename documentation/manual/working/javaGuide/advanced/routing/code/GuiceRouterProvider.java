/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static play.mvc.Results.ok;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import play.routing.RoutingDsl;

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
    return routingDsl
        .GET("/hello/:to")
        .routingTo((request, to) -> ok("Hello " + to))
        .build()
        .asScala();
  }
}
// #load-guice2
