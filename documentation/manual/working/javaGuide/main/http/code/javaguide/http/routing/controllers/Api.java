/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http.routing.controllers;

import java.util.Optional;

import play.mvc.Controller;
import play.mvc.Result;

public class Api extends Controller {
  public Result list(String version) {
    return ok("version " + version);
  }

  public Result listOpt(Optional<String> version) {
    return ok("version " + version.orElse("unknown"));
  }

  public Result newThing() {
    return ok();
  }
}
