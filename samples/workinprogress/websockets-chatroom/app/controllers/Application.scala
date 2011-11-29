package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.akka._

import actors._
import actors.ChatRoomActor._

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
  
  // -- Comet chat room
  
  def chatRoom = Action {
    Ok(views.html.room())
  }
  
  def stream = Action {
    AsyncResult {
      (ChatRoomActor.ref ? Join() map {_.asInstanceOf[Enumerator[String]]}).asPromise.map { chunks =>
        Ok(Comet(chunks, callback = "parent.message"))
      }
    }
  }
  
  def say(message: String) = Action {
    ChatRoomActor.ref ! Message(message)
    Ok("Said " + message)
  }

}