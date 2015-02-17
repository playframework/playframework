/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests

package controllers

import play.api.mvc._

object Application extends Controller {
  def index() = Action {
    Ok("Hello Bob") as("text/plain")
  }
}
