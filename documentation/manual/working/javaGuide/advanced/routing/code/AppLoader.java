/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.Application;
import play.ApplicationLoader;
import play.routing.Router;
import play.routing.RoutingDslComponentsFromContext;

import static play.mvc.Results.ok;

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
    return routingDsl().GET("/hello/:to").routeTo(to -> ok("Hello " + to)).build();
  }
}
// #load
