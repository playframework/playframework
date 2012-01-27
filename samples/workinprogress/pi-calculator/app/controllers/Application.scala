package controllers

import play.api._
import play.api.mvc._
import akka.actor._
import akka.actor._
import akka.util.duration._
import library.{Calculator,Work}
import play.api.mvc.Results._
import play.api.libs.concurrent._

import play.api.libs.concurrent._

object Application extends Controller {
  
  val system = ActorSystem("pi")
  val actor = system.actorOf(Props[Calculator])
  
  def index = Action {
    Ok(views.html.index())
  }

  def compute(start: Int, elements: Int) = Action {
    AsyncResult {
      (actor ? (Work(start, elements), 5.seconds)).mapTo[Double].asPromise.map { result =>
        Ok(views.html.computingResult(start, elements, result))
      }
    }
  }
  
}
