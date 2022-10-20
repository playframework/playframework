/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
