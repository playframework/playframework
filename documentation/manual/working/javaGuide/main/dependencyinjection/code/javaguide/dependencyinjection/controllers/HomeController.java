/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.dependencyinjection.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {
  public Result index() {
    return ok();
  }
}
