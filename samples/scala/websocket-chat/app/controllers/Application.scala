package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.iteratee._

import models._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def chatRoom(username: Option[String]) = Action {
    Ok(views.html.chatRoom(username.get))
  }
  
  def chat(username: String) = WebSocket[JsValue] { request => (in, out) =>
    
    // Get a reference on the chat room
    val chatRoom = ChatRoom.default
    
    val channel = new CallbackEnumerator[JsValue]
    
    channel |>> out
    
    // Join this room
    chatRoom ! Join(username, channel)
    
    // Create an Iteratee to consume the feed
    val iteratee = Iteratee.foreach[JsValue] { event =>
      chatRoom ! Talk(username, (event \ "text").as[String])
    }.mapDone { _ =>
      chatRoom ! Quit(username)
    }
    
    // Forward socket events to the room
    in |>> iteratee
    
  }
  
}