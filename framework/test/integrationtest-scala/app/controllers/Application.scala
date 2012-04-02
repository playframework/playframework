package controllers

import play.api._
import libs.json.Json
import play.api.mvc._

import play.api.Play.current

object Application extends Controller {
  
  def index(name: String) = Action {
      Ok(views.html.index(name))
  }
  
  def key = Action {
    Play.configuration.getString("key").map {
      key => Ok("Key=" + key)
    }.getOrElse(InternalServerError("Configuration missing"))
  }

  def json = Action { request =>
    request.body.asJson.map { json =>
      Ok(json)
    }.getOrElse(
        Ok(Json.toJson(Map("status" -> "KO", "message" -> "JSON Body missing")))
    )

  }
  
}