/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.controllers;

// #javascript-router-resource-imports
import play.routing.JavaScriptReverseRouter;
import play.mvc.Controller;
import play.mvc.Result;
// #javascript-router-resource-imports

public class Application extends Controller {

  // #javascript-router-resource
  public Result javascriptRoutes() {
    return ok(JavaScriptReverseRouter.create(
            "jsRoutes", routes.javascript.Users.list(), routes.javascript.Users.get()))
        .as("text/javascript");
  }
  // #javascript-router-resource

  public Result javascriptRoutes2() {
    return ok(
        // #javascript-router-resource-custom-method
        JavaScriptReverseRouter.create(
            "jsRoutes",
            "myAjaxMethod",
            routes.javascript.Users.list(),
            routes.javascript.Users.get())
        // #javascript-router-resource-custom-method
        )
        .as("text/javascript");
  }
}
