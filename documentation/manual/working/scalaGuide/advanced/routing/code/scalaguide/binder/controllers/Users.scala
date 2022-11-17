/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.binder.controllers

//#javascript-router-resource-imports
import javax.inject.Inject

import play.api.http.MimeTypes
import play.api.mvc._
import play.api.routing._
//#javascript-router-resource-imports

class Application @Inject() (components: ControllerComponents) extends AbstractController(components) {
  // #javascript-router-resource
  def javascriptRoutes: Action[AnyContent] = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Users.list,
        routes.javascript.Users.get
      )
    ).as(MimeTypes.JAVASCRIPT)
  }
  // #javascript-router-resource

  def javascriptRoutes2: Action[AnyContent] = Action { implicit request =>
    Ok(
      // #javascript-router-resource-custom-method
      JavaScriptReverseRouter("jsRoutes", Some("myAjaxFunction"))(
        routes.javascript.Users.list,
        routes.javascript.Users.get
      )
      // #javascript-router-resource-custom-method
    ).as(MimeTypes.JAVASCRIPT)
  }
}

class Users @Inject() (components: ControllerComponents) extends AbstractController(components) {
  def list: Action[AnyContent] = Action {
    Ok("List users")
  }

  def get(id: Long): Action[AnyContent] = Action {
    Ok(s"Get user with id $id")
  }
}
