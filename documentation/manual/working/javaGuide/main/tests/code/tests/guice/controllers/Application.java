/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests.guice.controllers;

import javaguide.tests.guice.Component;

// #controller
import play.mvc.*;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Application extends Controller {

  private final Component component;

  @Inject
  public Application(Component component) {
    this.component = component;
  }

  public Result index() {
    return ok(component.hello());
  }
}
// #controller
