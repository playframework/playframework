/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers;

import play.mvc.*;

public class InstanceController extends Controller {
  private int invoked = 0;

  public Result index() {
    this.invoked += 1;
    return ok("" + invoked);
  }
}
