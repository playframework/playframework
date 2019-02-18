/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._
import javax.inject.Inject

import play.filters.csrf.CSRF

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

  // #implicit-access-to-token
  def someAction = Action { implicit request =>
    accessToken // request is passed implicitly to accessToken
    Ok("success")
  }

  def accessToken(implicit request: Request[_]) = {
    val token = CSRF.getToken // request is passed implicitly to CSRF.getToken
  }
  // #implicit-access-to-token
}
