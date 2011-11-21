package controllers

import play.api._
import play.api.mvc._

import play.api.libs.iteratee._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def echo = WebSocket[String] { request => (in,out) =>
    out <<: in.map {
      case EOF => {
        Logger.info("Cleaning resources")
        EOF
      }
      case el => {
        Logger.info("Got message: " + el)
        el.map(_.reverse)
      }
    }    
  }
  
}