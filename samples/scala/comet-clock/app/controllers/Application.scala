package controllers

import play.api._
import play.api.mvc._

import play.api.libs.{ Comet }
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.duration._

object Application extends Controller {
  
  /** 
   * A String Enumerator producing a formatted Time message every 100 millis.
   * A callback enumerator is pure an can be applied on several Iteratee.
   */
  lazy val clock: Enumerator[String] = {
    
    import java.util._
    import java.text._
    
    val dateFormat = new SimpleDateFormat("HH mm ss")
    
    Enumerator.fromCallback { () =>
      Promise.timeout(Some(dateFormat.format(new Date)), 100 milliseconds)
    }
  }
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def liveClock = Action {
    Ok.stream(clock &> Comet(callback = "parent.clockChanged"))
  }
  
}
