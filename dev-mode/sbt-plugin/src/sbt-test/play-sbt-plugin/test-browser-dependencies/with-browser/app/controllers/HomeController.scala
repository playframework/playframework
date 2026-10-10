/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import jakarta.inject.Inject
import play.api.mvc._

class HomeController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def index: Action[AnyContent] = Action { (request: Request[AnyContent]) =>
    val email = request.getQueryString("email").getOrElse("")
    Ok(s"""<html><body><h1 id="greeting">Hello browser</h1>
          |<form id="form" method="get" action="/"><input name="email"></form>
          |<p id="received">$email</p></body></html>""".stripMargin).as(HTML)
  }
}
