/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static play.mvc.Results.ok;

import play.Application;
import play.ApplicationLoader;
import play.routing.Router;
import play.routing.RoutingDslComponentsFromContext;

// #load
public class AppLoader implements ApplicationLoader {
  public Application load(ApplicationLoader.Context context) {
    return new MyComponents(context).application();
  }
}

class MyComponents extends RoutingDslComponentsFromContext
    implements play.filters.components.NoHttpFiltersComponents {

  MyComponents(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return routingDsl().GET("/hello/:to").routingTo((request, to) -> ok("Hello " + to)).build();
  }
}
// #load
