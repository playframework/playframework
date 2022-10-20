/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di.controllers;

import play.mvc.*;

public class Application extends Controller {
  public Result index() {
    return ok();
  }
}
