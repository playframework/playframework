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
  
  def chat(username: String) = WebSocket.using{ request =>
    
    val (socketListner,socketWriter) = ChatRoom.join(username)

    (socketListner,socketWriter)
  }
  
}
