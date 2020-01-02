/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.dependencyinjection.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {
  public Result index() {
    return ok();
  }
}
