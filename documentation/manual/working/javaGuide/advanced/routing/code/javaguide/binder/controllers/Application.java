/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.controllers;

// #javascript-router-resource-imports
import play.mvc.Http;
import play.routing.JavaScriptReverseRouter;
import play.mvc.Controller;
import play.mvc.Result;
// #javascript-router-resource-imports

public class Application extends Controller {

  // #javascript-router-resource
  public Result javascriptRoutes(Http.Request request) {
    return ok(JavaScriptReverseRouter.create(
            "jsRoutes",
            request.host(),
            routes.javascript.Users.list(),
            routes.javascript.Users.get()))
        .as(Http.MimeTypes.JAVASCRIPT);
  }
  // #javascript-router-resource
}
