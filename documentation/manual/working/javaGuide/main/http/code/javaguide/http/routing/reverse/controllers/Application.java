/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// #controller
// ###replace: package controllers;
package javaguide.http.routing.reverse.controllers;

import play.*;
import play.mvc.*;

public class Application extends Controller {

  public Result hello(String name) {
    return ok("Hello " + name + "!");
  }
}
// #controller
