package controllers

import play.Logger._

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import play.api.libs.json._

object Tests extends Controller {

  def show = Action { implicit request =>
    import play.filters.csrf._
    val token = CSRF.getToken(request)
    trace("CSRF TOKEN: " + token)
    Ok(token.map(_.value).getOrElse(""))
  }

  def post = Action { implicit request =>
    Ok(request.body.toString)
  }

}