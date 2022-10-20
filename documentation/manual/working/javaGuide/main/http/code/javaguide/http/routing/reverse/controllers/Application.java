/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
