package controllers

import akka.util.duration._
import play.api._
import play.api.mvc._
import play.api.libs._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

import actors._
import actors.ChatRoomActor._
import akka.util.Timeout
import akka.pattern.ask

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index()) 
  }
  
  // -- Comet chat room
  
  def chatRoom = Action { implicit request =>
    Ok(views.html.room())
  }
  
  def stream = Action {
    AsyncResult {
      implicit val timeout = Timeout(5.seconds)
      (ChatRoomActor.ref ? (Join()) ).mapTo[Enumerator[String]].asPromise.map { chunks =>
        Ok.stream(chunks &> Comet( callback = "parent.message"))
      }
    }
  }
  
  def say(message: String) = Action {
    ChatRoomActor.ref ! Message(message)
    Ok("Said " + message)
  }

}
