package play.api.db.slick.mvc

import play.api.mvc._
import play.api._
import scala.concurrent._
import play.api.Play.current
import play.api.db.slick._

trait SlickController { self: Controller =>

  val slickExecutionContext = SlickExecutionContext.executionContext

  def SlickAction(r: => Result) = {
    Action {
      Async {
        Future(r)(slickExecutionContext)
      }
    }
  }

  def SlickAction(r: (Request[play.api.mvc.AnyContent]) => Result) = {
    Action { implicit request =>
      Async {
        Future(r(request))(slickExecutionContext)
      }
    }
  }

}