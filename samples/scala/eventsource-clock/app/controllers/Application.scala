package controllers

import play.api._
import play.api.mvc._

import play.api.libs.{ EventSource }
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Application extends Controller {
  
  /** 
   * A String Enumerator producing a formatted Time message every 100 millis.
   * A callback enumerator is pure an can be applied on several Iteratee.
   */
  lazy val clock: Enumerator[String] = {
    
    import java.util._
    import java.text._
    
    val dateFormat = new SimpleDateFormat("HH mm ss")
    
    Enumerator.generateM {
      Promise.timeout(Some(dateFormat.format(new Date)), 100 milliseconds)
    }
  }
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def liveClock = Action {
    Ok.chunked(clock &> EventSource()).as("text/event-stream")
  }
  
}
