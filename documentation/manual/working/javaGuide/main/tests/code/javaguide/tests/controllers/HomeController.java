/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.tests.controllers;

import play.mvc.*;

public class HomeController extends Controller {

  public Result index() {
    return ok(javaguide.tests.html.index.render("Welcome to Play!"));
  }
  
}
