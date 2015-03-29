/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def config = Action {
    Ok(Play.configuration.underlying.getString("some.config"))
  }
}
