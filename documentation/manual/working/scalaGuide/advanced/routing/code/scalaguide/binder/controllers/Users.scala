/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.binder.controllers

//#javascript-router-resource-imports
import javax.inject.Inject
import play.api.http.MimeTypes
import play.api.mvc._
import play.api.routing._
//#javascript-router-resource-imports

class Application @Inject() (components: ControllerComponents) extends AbstractController(components) {
  //#javascript-router-resource
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Users.list,
        routes.javascript.Users.get
      )
    ).as(MimeTypes.JAVASCRIPT)
  }
  //#javascript-router-resource
}

class Users @Inject() (components: ControllerComponents) extends AbstractController(components) {
  def list = Action {
    Ok("List users")
  }

  def get(id: Long) = Action {
    Ok(s"Get user with id $id")
  }
}
