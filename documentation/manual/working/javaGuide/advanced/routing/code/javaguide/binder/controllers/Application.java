/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.binder.controllers;

//#javascript-router-resource-imports
import play.mvc.Controller;
import play.mvc.Result;
import play.Routes;
//#javascript-router-resource-imports

// import static javaguide.binder.controllers.routes;

public class Application extends Controller {
    
    //#javascript-router-resource
    public Result javascriptRoutes() {
        return ok(
            Routes.javascriptRouter("jsRoutes",
                routes.javascript.Users.list(),
                routes.javascript.Users.get()
            )
        ).as("text/javascript");
    }
    //#javascript-router-resource

    public Result javascriptRoutes2() {
        return ok(
            //#javascript-router-resource-custom-method
            Routes.javascriptRouter("jsRoutes", "myAjaxMethod",
                routes.javascript.Users.list(),
                routes.javascript.Users.get()
            )
            //#javascript-router-resource-custom-method
        ).as("text/javascript");
    }
}
