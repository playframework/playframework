/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._

class Application extends Controller {

  /**
   * This action echoes the value of the HTTP_SERVER tag so that we
   * can test if we're using the Akka HTTP server.
   */
  def index = Action { request =>
    val httpServerTag = request.tags.getOrElse("HTTP_SERVER", "unknown")
    Ok(s"HTTP_SERVER tag: $httpServerTag")
  }
}
