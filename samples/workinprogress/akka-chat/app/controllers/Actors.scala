package actors

import play.api._

import akka.actor._
import akka.actor.Actor._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ChatRoomActor extends Actor {
  
  import ChatRoomActor._
  
  var members = Seq.empty[Channel[String]]

  def receive = {
    
    case Join() => {
      Logger.info("New member joined")
      @volatile var channel: Channel[String] = null
      sender ! Concurrent.unicast[String](
        onStart = { c: Channel[String] =>
          channel = c
          self ! Started(c)
        },
        onComplete = { () =>
          self ! Quit(channel)
        }
      )
    }

    case Started(channel) => {
      Logger.info("New member started")
      members = members :+ channel
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
  case class Started(channel: Channel[String]) extends Event
  case class Quit(channel: Channel[String]) extends Event
  case class Message(msg: String) extends Event
  lazy val system = ActorSystem("chatroom")
  lazy val ref = system.actorOf(Props[ChatRoomActor])
  
}
