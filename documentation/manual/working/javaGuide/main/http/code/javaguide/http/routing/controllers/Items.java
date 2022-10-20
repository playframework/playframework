/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http.routing.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Items extends Controller {
  public Result show(Long id) {
    return ok("showing item " + id);
  }
}
