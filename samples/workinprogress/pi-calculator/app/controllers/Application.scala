package controllers

import play.api._
import akka.util.Timeout
import akka.pattern.ask
import play.api.mvc._
import akka.actor._
import akka.actor._
import scala.concurrent.duration._
import library.{Calculator,Work}
import play.api.mvc.Results._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller {
  
  val system = ActorSystem("pi")
  val actor = system.actorOf(Props[Calculator])
  
  def index = Action {
    Ok(views.html.index())
  }

  def compute(start: Int, elements: Int) = Action {
    AsyncResult {
      implicit val timeout= Timeout(5.seconds)
      (actor ? Work(start, elements) ).mapTo[Double].map { result =>
        Ok(views.html.computingResult(start, elements, result))
      }
    }
  }
  
}
