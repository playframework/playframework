/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.mvc.*;

public class InstanceController extends Controller {
  private int invoked = 0;

  public Result index(Http.Request req) {
    this.invoked += 1;
    return ok(req.uri() + " " + invoked);
  }
}
