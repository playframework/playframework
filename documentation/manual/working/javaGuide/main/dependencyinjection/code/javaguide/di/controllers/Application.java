/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.controllers;

import play.mvc.*;

public class Application extends Controller {
  public Result index() {
    return ok();
  }
}
