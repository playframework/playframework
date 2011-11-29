package actors

import play.api._

import akka.actor._
import akka.actor.Actor._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

class ChatRoomActor extends Actor {
  
  import ChatRoomActor._
  
  var members = Seq.empty[CallbackEnumerator[String]]
  
  def receive = {
    
    case Join() => {
      lazy val channel:CallbackEnumerator[String] = new CallbackEnumerator[String](
        onComplete = {
          Logger.info("Member has disconnected: " + channel)
          members = members.filterNot(_ == channel)
        }
      )
      members = members :+ channel
      Logger.info("New member joined")
      self.reply(channel)
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
  case class Message(msg: String) extends Event
  
  lazy val ref = actorOf[ChatRoomActor]
  
}
