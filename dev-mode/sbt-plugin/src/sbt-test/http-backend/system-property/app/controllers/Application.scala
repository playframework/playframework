/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.mvc.request.RequestAttrKey

class Application @Inject()(c: ControllerComponents) extends AbstractController(c) {

  /**
   * This action echoes the value of the HTTP_SERVER tag so that we
   * can test if we're using the Akka HTTP server.
   */
  def index = Action { request =>
    val httpServerTag = request.attrs.get(RequestAttrKey.Server).getOrElse("unknown")
    Ok(s"HTTP_SERVER tag: $httpServerTag")
  }
}
