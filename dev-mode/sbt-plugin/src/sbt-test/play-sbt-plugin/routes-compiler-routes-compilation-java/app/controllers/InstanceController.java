/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import jakarta.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public class InstanceController extends Controller {

  @Inject
  public InstanceController() {}

  public Result index() {
    return ok();
  }
}
