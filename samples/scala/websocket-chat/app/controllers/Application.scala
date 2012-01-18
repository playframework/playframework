package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.iteratee._

import models._

import akka.actor._
import akka.util.duration._

object Application extends Controller {
  
  /**
   * Just display the home page.
   */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }
  
  /**
   * Display the chat room page.
   */
  def chatRoom(username: Option[String]) = Action { implicit request =>
    username.filterNot(_.isEmpty).map { username =>
      Ok(views.html.chatRoom(username))
    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Please choose a valid username."
      )
    }
  }
  
  /**
   * Handles the chat websocket.
   */
  def chat(username: String) = WebSocket[JsValue] { request => (in, out) =>
    
    // Get a reference on the chat room
    val chatRoom = ChatRoom.default
    
    // Create an Enumerator to write to this socket
    val channel = new CallbackEnumerator[JsValue]
    
    // Apply this Enumerator on the socket out.
    channel |>> out
    
    // Join this room
    chatRoom ? (Join(username, channel), 1 second) map {
      
      case Connected() => 
      
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          chatRoom ! Talk(username, (event \ "text").as[String])
        }.mapDone { _ =>
          chatRoom ! Quit(username)
        }

        // Forward socket events to the Iteratee
        in |>> iteratee
        
      case CannotConnect(error) => 
      
        // Connection error
        channel.push(JsObject(Seq("error" -> JsString(error))))
      
    }
    
  }
  
}
