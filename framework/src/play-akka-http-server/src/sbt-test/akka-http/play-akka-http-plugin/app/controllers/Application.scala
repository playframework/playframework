/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.mvc._

object Application extends Controller {

  /**
   * This action echoes the value of the HTTP_SERVER tag so that we
   * can test if we're using the Akka HTTP server.
   */
  def index = Action { request =>
    val httpServerTag = request.tags.getOrElse("HTTP_SERVER", "unknown")
    Ok(s"HTTP_SERVER tag: $httpServerTag")
  }
}
