/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.controllers;

// #javascript-router-resource-imports
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.JavaScriptReverseRouter;

// #javascript-router-resource-imports

public class Application extends Controller {

  // #javascript-router-resource
  public Result javascriptRoutes(Http.Request request) {
    return ok(JavaScriptReverseRouter.create(
            "jsRoutes",
            "jQuery.ajax",
            request.host(),
            routes.javascript.Users.list(),
            routes.javascript.Users.get()))
        .as(Http.MimeTypes.JAVASCRIPT);
  }
  // #javascript-router-resource

  public Result javascriptRoutes2(Http.Request request) {
    return ok(
        // #javascript-router-resource-custom-method
        JavaScriptReverseRouter.create(
            "jsRoutes",
            "myAjaxMethod",
            request.host(),
            routes.javascript.Users.list(),
            routes.javascript.Users.get())
        // #javascript-router-resource-custom-method
        )
        .as(Http.MimeTypes.JAVASCRIPT);
  }
}
