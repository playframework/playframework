package actors

import play.api._

import akka.actor._
import akka.actor.Actor._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

class ChatRoomActor extends Actor {
  
  import ChatRoomActor._
  
  var members = Seq.empty[PushEnumerator[String]]

  def receive = {
    
    case Join() => {
      lazy val channel: PushEnumerator[String] =  Enumerator.imperative[String](
        onComplete = self ! Quit(channel)
      )
      members = members :+ channel
      Logger.info("New member joined")
      sender ! channel
    }
    
    case Quit(channel) => {
      Logger.info("Member has disconnected: " + channel)
      members = members.filterNot(_ == channel)
    }
    
    case Message(msg) => {
      Logger.info("Got message, send it to " + (Option(members).filterNot(_.isEmpty).map(_.size + " members").getOrElse("no one")))
      members.foreach(_.push(msg))
    }
    
  }
  
}

object ChatRoomActor {
  
  trait Event
  case class Join() extends Event
  case class Quit(channel: PushEnumerator[String]) extends Event
  case class Message(msg: String) extends Event
  lazy val system = ActorSystem("chatroom")
  lazy val ref = system.actorOf(Props[ChatRoomActor])
  
}
