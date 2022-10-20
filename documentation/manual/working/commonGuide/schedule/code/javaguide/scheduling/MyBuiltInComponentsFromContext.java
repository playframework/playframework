/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
