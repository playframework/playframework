/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import models.Unbindable;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

  public Result index() {
    return ok();
  }

  /** Play can't handle the return type. */
  public String text() {
    return "text";
  }

  public Result item(Unbindable id) {
    return ok();
  }

  public Result fixedValue(Long x) {
    return ok();
  }

  public Result defaultValue(Long x) {
    return ok();
  }
}
