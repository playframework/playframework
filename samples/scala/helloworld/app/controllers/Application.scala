package controllers

import play.api._
import play.api.mvc._
import play.api.data._

import views._

object Application extends Controller {

  val helloForm = Form(of(
      "name" -> requiredText,
      "repeat" -> number(min = 1, max = 100),
      "color" -> optional(text)
  ))

  // -- Actions

  def index = Action {
    Ok(html.index(helloForm))
  }

  def sayHello = Action { implicit request =>
    helloForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.index(formWithErrors)),
      {case (name, repeat, color) => Ok(html.hello(name, repeat.toInt, color))}
    )
  }

}
