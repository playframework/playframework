/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// ###replace: package tasks;
package javaguide.scheduling;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.NoHttpFiltersComponents;
import play.routing.Router;

public class MyBuiltInComponentsFromContext extends BuiltInComponentsFromContext
    implements NoHttpFiltersComponents {

  public MyBuiltInComponentsFromContext(ApplicationLoader.Context context) {
    super(context);

    this.initialize();
  }

  private void initialize() {
    // Task is initialize here
    new CodeBlockTask(actorSystem(), executionContext());
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
