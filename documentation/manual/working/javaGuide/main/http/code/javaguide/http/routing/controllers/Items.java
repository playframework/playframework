/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Items extends Controller {
  public Result show(Long id) {
    return ok("showing item " + id);
  }
}
