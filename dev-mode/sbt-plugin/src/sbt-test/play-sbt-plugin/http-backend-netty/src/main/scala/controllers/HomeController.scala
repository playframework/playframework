/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import javax.inject._
import play.api._
import play.api.mvc._
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Successful response.")
  }
}
