package scalaguide.tests

package controllers

import play.api.mvc._

object Application extends Controller {
  def index() = Action {
    Ok("Hello Bob") as("text/plain")
  }
}
