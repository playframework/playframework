package controllers

import javax.inject.Inject

import play.api.mvc._

/**
 * A very small controller that renders a home page.
 */
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok("Hello, this is Play!")
  }
}
