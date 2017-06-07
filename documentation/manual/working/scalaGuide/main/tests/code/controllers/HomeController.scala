/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests

package controllers

import play.api.mvc._

class HomeController extends Controller {
  def index() = Action {
    Ok("Hello Bob") as("text/plain")
  }
}
