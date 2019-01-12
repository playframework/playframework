/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.binder.controllers;

//#javascript-router-resource-imports
import play.mvc.Http;
import play.routing.JavaScriptReverseRouter;
import play.mvc.Controller;
import play.mvc.Result;
//#javascript-router-resource-imports

public class Application extends Controller {
    
    //#javascript-router-resource
    public Result javascriptRoutes(Http.Request request) {
        return ok(
            //###skip: 1
            // TODO: After Play 2.7 use create(String name, String host, JavaScriptReverseRoute... routes) instead
            JavaScriptReverseRouter.create("jsRoutes", "jQuery.ajax", request.host(),
                routes.javascript.Users.list(),
                routes.javascript.Users.get()
            )
        ).as(Http.MimeTypes.JAVASCRIPT);
    }
    //#javascript-router-resource

    public Result javascriptRoutes2(Http.Request request) {
        return ok(
            //#javascript-router-resource-custom-method
            JavaScriptReverseRouter.create("jsRoutes", "myAjaxMethod", request.host(),
                routes.javascript.Users.list(),
                routes.javascript.Users.get()
            )
            //#javascript-router-resource-custom-method
        ).as(Http.MimeTypes.JAVASCRIPT);
    }
}
