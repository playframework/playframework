/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.components;

// #scheduler-compile-time-injection
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.components.AkkaTypedComponents;
import play.controllers.AssetsComponents;
import play.filters.components.HttpFiltersComponents;
import play.routing.Router;

public class ComponentsWithTypedScheduler extends BuiltInComponentsFromContext
    implements AkkaTypedComponents, AssetsComponents, HttpFiltersComponents {

  public ComponentsWithTypedScheduler(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
// #scheduler-compile-time-injection
