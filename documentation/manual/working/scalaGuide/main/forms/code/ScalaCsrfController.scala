/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._
import javax.inject.Inject

class ScalaCsrfController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  // #some-csrf-action
  // this actions needs to access CSRF token
  def someMethod = Action { implicit request =>
      // access the token as you need
      Ok
    }
  // #some-csrf-action

  // #some-csrf-action-with-more-methods
  def action = Action { implicit request =>
    anotherMethod("Some para value")
    Ok
  }

  def anotherMethod(p: String)(implicit request: Request[_]) = {
    // do something that needs access to the request
  }
  // #some-csrf-action-with-more-methods
}
