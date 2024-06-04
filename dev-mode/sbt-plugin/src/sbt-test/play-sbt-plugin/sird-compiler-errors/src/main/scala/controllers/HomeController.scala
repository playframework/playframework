/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import jakarta.inject.Inject

import play.api.mvc._

/**
 * A very small controller that renders a home page.
 */
class HomeController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index: Action[AnyContent] = Action { implicit request =>
    Ok("Hello, this is Play!")
  }
}
