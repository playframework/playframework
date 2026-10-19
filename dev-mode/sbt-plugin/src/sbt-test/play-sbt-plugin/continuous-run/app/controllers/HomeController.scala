/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import example.Greeting
import jakarta.inject.Inject
import play.api.mvc._
import play.api.Configuration

class HomeController @Inject() (cc: ControllerComponents, configuration: Configuration) extends AbstractController(cc) {
  def index = Action {
    Ok(Greeting.text + ":" + views.html.index().body + ":" + configuration.get[String]("label"))
  }
}
