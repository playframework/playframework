/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5.guice.controllers;

import javaguide.test.junit5.guice.Component;

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
