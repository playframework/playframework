package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import play.api.libs.iteratee._
import play.api.libs.concurrent._


object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index()) 
  }
  
  // -- Echo websocket
  
  def echo(name: String) = WebSocket[String] { request => (in, out) =>
    
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
