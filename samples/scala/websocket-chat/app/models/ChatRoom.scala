package models

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.akka._
import play.api.libs.iteratee._

import play.api.Play.current

object Robot {
  
  def apply(chatRoom: ActorRef) {
    
    val loggerIteratee = Iteratee.foreach[JsValue](event => Logger("robot").info(event.toString))
    
    val robotChannel = new CallbackEnumerator[JsValue]
    
    robotChannel |>> loggerIteratee
    
    chatRoom ! Join("Robot", robotChannel)
    
    Akka.system.scheduler.schedule(
      10 seconds,
      10 seconds,
      chatRoom,
      Talk("Robot", "I'm still alive")
    )
  }
  
}

object ChatRoom {
  
  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[ChatRoom])
    
    // Create a bot user (just for fun)
    Robot(roomActor)
    
    roomActor
  }

  def join(username:String): (Iteratee[JsValue,_], Enumerator[JsValue]) = {

    // Create an imperative enumerator to push messages into the socket                                        
    val channel = new CallbackEnumerator[JsValue]

    // Create an Iteratee to consume messages from the user and push them to the chatroom
    val iteratee = Iteratee.foreach[JsValue] { event =>
      default ! Talk(username, (event \ "text").as[String])
    }.mapDone { _ =>
      default ! Quit(username)
    }

    // Join this room
    default ! Join(username, channel)

    (iteratee,channel)

  }
  
}

class ChatRoom extends Actor {
  
  var members = Map.empty[String, CallbackEnumerator[JsValue]]
  
  def receive = {
    
    case Join(username, channel) => {
      members = members + (username -> channel)
      notifyAll("join", username, "has joined") 
    }
    
    case Talk(username, text) => {
      notifyAll("talk", username, text)
    }
    
    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has quitted")
    }
    
  }
  
  def notifyAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          members.keySet.toList.map(JsString)
        )
      )
    )
    members.foreach { 
      case (_, channel) => channel.push(msg)
    }
  }
  
}

case class Join(username: String, channel: CallbackEnumerator[JsValue])
case class Quit(username: String)
case class Talk(username: String, text: String)
