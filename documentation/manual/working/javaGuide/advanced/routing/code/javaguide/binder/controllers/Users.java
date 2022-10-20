/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Users extends Controller {

  public Result list() {
    return ok("List Users");
  }

  public Result get(Long id) {
    return ok("Get user by id");
  }
}
