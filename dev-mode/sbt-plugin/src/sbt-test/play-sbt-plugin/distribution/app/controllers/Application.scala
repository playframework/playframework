/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import jakarta.inject.Inject
import play.api._
import play.api.mvc._

class Application @Inject() (env: Environment, configuration: Configuration, c: ControllerComponents)
    extends AbstractController(c) {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def config = Action {
    Ok(configuration.underlying.getString("some.config"))
  }

  def count = Action {
    val num = env.resource("application.conf").toSeq.size
    Ok(num.toString)
  }
}
