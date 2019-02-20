/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters.removeAll;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.NoHttpFiltersComponents;
import play.routing.Router;

// #remove-all-filters-compile-time-di
public class MyAppComponents extends BuiltInComponentsFromContext
    implements NoHttpFiltersComponents {

  public MyAppComponents(ApplicationLoader.Context context) {
    super(context);
  }

  // no need to override httpFilters method

  @Override
  public Router router() {
    return Router.empty(); // implement the router as needed
  }
}
// #remove-all-filters-compile-time-di
