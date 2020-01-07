/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests.controllers;

import play.mvc.*;

public class HomeController extends Controller {

  public Result index() {
    return ok(javaguide.tests.html.index.render("Welcome to Play!"));
  }

  public Result post(Http.Request request) {
    return redirect(routes.HomeController.index());
  }
}
