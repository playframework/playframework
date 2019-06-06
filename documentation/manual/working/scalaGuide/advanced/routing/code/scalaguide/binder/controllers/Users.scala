/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.binder.controllers

//#javascript-router-resource-imports
import javax.inject.Inject

import play.api.mvc._
import play.api.routing._
//#javascript-router-resource-imports

class Application @Inject()(components: ControllerComponents) extends AbstractController(components) {
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

class Users @Inject()(components: ControllerComponents) extends AbstractController(components) {

  def list = Action {
    Ok("List users")
  }

  def get(id: Long) = Action {
    Ok(s"Get user with id $id")
  }
}
