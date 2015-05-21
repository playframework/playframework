/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import scala.collection.JavaConverters._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def config = Action {
    Ok(Play.configuration.underlying.getString("some.config"))
  }

  def count = Action {
    val num = Play.classloader.getResources("application.conf").asScala.toSeq.size
    Ok(num.toString)
  }
}
