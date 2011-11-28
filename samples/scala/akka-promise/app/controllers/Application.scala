package controllers

import play.api._
import play.api.mvc._
import akka.actor.Actor._
import library.{Calculator,Work}
import play.api.mvc.Results._
import play.api.libs.akka._

object Application extends Controller {
  
  val actor = actorOf[Calculator].start

  def index = Action{ 
    val future = (actor ? Work(4,4)).mapTo[Result] 
    AsyncResult(future.asPromise)
  }
}
