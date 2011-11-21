package controllers

import play.api._
import play.api.mvc._

import play.api.libs.iteratee._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def echo(name: String) = WebSocket[String] { request => (in,out) =>
    
    Logger.info(name + " is connected!")
    
    out <<: in.map {
      case EOF => {
        Logger.info(name + " is disconnected. Cleaning resources")
        EOF
      }
      case el => {
        Logger.info("Got message: " + el)
        el.map("[" + name + "] " + _.reverse)
      }
    }    
  }
  
}