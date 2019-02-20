/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.controllers;

import play.mvc.*;

public class Application extends Controller {
  public Result index() {
    return ok();
  }
}
