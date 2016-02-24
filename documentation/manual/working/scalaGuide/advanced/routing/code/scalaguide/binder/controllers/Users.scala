/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.binder.controllers

import play.api.Play.current
//#javascript-router-resource-imports
import play.api.mvc._
import play.api.routing._
//#javascript-router-resource-imports

class Application extends Controller {
    //#javascript-router-resource
    def javascriptRoutes = Action { implicit request =>
        Ok(
            JavaScriptReverseRouter("jsRoutes")(
                routes.javascript.Users.list,
                routes.javascript.Users.get
            )
        ).as("text/javascript")
    }
    //#javascript-router-resource

    def javascriptRoutes2 = Action { implicit request =>
        Ok(
            //#javascript-router-resource-custom-method
            JavaScriptReverseRouter("jsRoutes", Some("myAjaxFunction"))(
                routes.javascript.Users.list,
                routes.javascript.Users.get
            )
            //#javascript-router-resource-custom-method
        ).as("text/javascript")
    }
    
}

class Users extends Controller {
    
    def list = Action {
        Ok("List users")
    }

    def get(id: Long) = Action {
        Ok(s"Get user with id $id")
    }
}
