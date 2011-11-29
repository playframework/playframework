package controllers

import play.api._
import play.api.mvc._
import akka.actor.Actor._
import library.{Calculator,Work}
import play.api.mvc.Results._
import play.api.libs.akka._

import play.api.libs.concurrent._

object Application extends Controller {
  
  val actor = actorOf[Calculator].start
  
  def index = Action {
    Ok(views.html.index())
  }

  def compute(start: Int, elements: Int) = Action {
    AsyncResult {
      (actor ? Work(start, elements)).mapTo[Double].asPromise.map { result =>
        Ok(views.html.computingResult(start, elements, result))
      }
    }
  }
  
}
