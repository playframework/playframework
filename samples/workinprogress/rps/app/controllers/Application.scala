package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import scala.concurrent.stm._

object Application extends Controller {
  
  def index = Action {
    SpeedOMeter.countRequest()
    Ok("ok")
  }

  def monitor = Action {
    Ok(views.html.monitor(Runtime.getRuntime().totalMemory()/(1024*1024)))
  }

  def speedMeter = Action {
   Ok.stream( 
     Streams.getRequestsPerSecond >-
     Streams.getHeap &>
     Comet( callback = "parent.message"))
  }

  def gc = Action {
    Runtime.getRuntime().gc()
    Ok("ok")
  }
}

object Streams {

  val getRequestsPerSecond = Enumerator.callbackEnumerator{ () =>
    Promise.timeout( { 
      val currentMillis = java.lang.System.currentTimeMillis()      
      Some(SpeedOMeter.getSpeed +":rps") },
      100, TimeUnit.MILLISECONDS )
    }

  val getHeap = Enumerator.callbackEnumerator{ () =>
    Promise.timeout( 
      Some((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024) + ":memory"),
      100, TimeUnit.MILLISECONDS) 
  }

}

object SpeedOMeter {

  val unit = 100

  private val counter = Ref((0,(0,java.lang.System.currentTimeMillis())))

  def countRequest() = {
    val current = java.lang.System.currentTimeMillis()
    counter.single.transform {
      case (precedent,(count,millis)) if current > millis + unit => (0, (1,current))
      case (precedent,(count,millis)) if current > millis + (unit/2) => (count, (1, current))
      case (precedent,(count,millis))  => (precedent,(count + 1, millis))
    }
  } 


  def getSpeed = {
    val current = java.lang.System.currentTimeMillis()
    val (precedent,(count,millis)) = counter.single()
    val since = current-millis
    if(since <= unit) ((count + precedent) * 1000) / (since + unit/2)
    else 0
  }

}


