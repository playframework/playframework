/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
import play.api.ApplicationLoader.Context;
import play.api.*;
import play.api.http.HttpFilters;
import play.api.mvc.EssentialFilter;
import play.api.routing.Router;
import play.routing.RoutingDslComponentsFromContext;
import scala.collection.Seq;

import static play.mvc.Results.*;

//#load
public class AppLoader implements ApplicationLoader {
  public Application load(Context context) {
    return new MyComponents(context).application();
  }
}

class MyComponents extends RoutingDslComponentsFromContext {

  MyComponents(Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return routingDsl()
            .GET("/hello/:to").routeTo(to -> ok("Hello " + to))
            .build()
            .asScala();
  }

  @Override
  public HttpFilters defaultFilters() {
    return () -> null;
  }
}
//#load
