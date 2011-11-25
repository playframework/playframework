package actors

import play.api._

import akka.actor._
import akka.actor.Actor._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

class HelloActor extends Actor {
  
  var members = Seq.empty[PushIntoEnumerator[String]]
  
  def receive = {
    
    case "join" => {
      val channel = new PushIntoEnumerator[String](
        onComplete = c => {
          Logger.info("Member has disconnected: " + c)
          members = members.filterNot(_ == c)
        }
      )
      members = members :+ channel
      Logger.info("New member joined")
      self.reply(channel)
    }
    
    case msg:String => {
      Logger.info("Got message, send it to " + (Option(members).filterNot(_.isEmpty).map(_.size + " members").getOrElse("no one")))
      members.foreach(_.push(msg))
    }
    
  }
  
}

object HelloActor {
  
  lazy val ref = actorOf[HelloActor]
  
}

class PushIntoEnumerator[E](
  onComplete: (PushIntoEnumerator[E]) => Unit = (_:PushIntoEnumerator[E]) => (), 
  onError: (PushIntoEnumerator[E], String,Input[E]) => Unit = (_:PushIntoEnumerator[E], _:String,_:Input[E]) => () 
) extends Enumerator[E] {
  
  var iteratee:Iteratee[E,_] = _
  var promise:Promise[Iteratee[E,_]] with Redeemable[Iteratee[E,_]] = _
  
  def apply[A, EE >: E](it: Iteratee[EE,A]): Promise[Iteratee[EE,A]] = {
    iteratee = it.asInstanceOf[Iteratee[E,_]]
    val newPromise = new STMPromise[Iteratee[EE,A]]()
    promise = newPromise.asInstanceOf[Promise[Iteratee[E,_]] with Redeemable[Iteratee[E,_]]]
    newPromise
  }
  
  def close() {
    if(iteratee == null) {
      iteratee.feed(EOF).map { result =>
        promise.redeem(result)
      }
      iteratee = null
      promise = null
    }
  }
  
  def push(item: E): Boolean = {
    if(iteratee != null) {
      iteratee = iteratee.flatFold[E, Any](

        // DONE
        (a, in) => {
          onComplete(this)
          Promise.pure(Done(a, in))
        },

        // CONTINUE
        k => {
          Promise.pure(k(El(item)))
        },

        // ERROR
        (e, in) => {
          onError(this, e, in)
          Promise.pure(Error(e,in))
        }

      )
      true
    } else {
      false
    }
  }
  
}