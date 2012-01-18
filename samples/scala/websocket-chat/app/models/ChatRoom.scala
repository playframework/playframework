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
    
    // Create an Iteratee that log all messages to the console.
    val loggerIteratee = Iteratee.foreach[JsValue](event => Logger("robot").info(event.toString))
    
    // Create an Enemurator for the Robot. 
    val robotChannel = new CallbackEnumerator[JsValue]
    
    // Apply this Enumerator on the logger.
    robotChannel |>> loggerIteratee
    
    // Make the robot join the room
    chatRoom ! Join("Robot", robotChannel)
    
    // Make the robot talk every 30 seconds
    Akka.system.scheduler.schedule(
      30 seconds,
      30 seconds,
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
  
}

class ChatRoom extends Actor {
  
  var members = Map.empty[String, CallbackEnumerator[JsValue]]
  
  def receive = {
    
    case Join(username, channel) => {
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + (username -> channel)
        notifyAll("join", username, "has entered the room")
        sender ! Connected()
      }
    }
    
    case Talk(username, text) => {
      notifyAll("talk", username, text)
    }
    
    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has leaved the room")
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

case class Connected()
case class CannotConnect(msg: String)
