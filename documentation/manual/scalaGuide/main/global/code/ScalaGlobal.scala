/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.global.scalaglobal {

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.core.Router

@RunWith(classOf[JUnitRunner])
class ScalaGlobalSpec extends Specification {

  "A scala global" should {

    "hooking global to log when start and stop" in {

      //#global-hooking
      import play.api._

      object Global extends GlobalSettings {

        override def onStart(app: Application) {
          Logger.info("Application has started")
        }

        override def onStop(app: Application) {
          Logger.info("Application shutdown...")
        }

      }
      //#global-hooking

      running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"),withGlobal=Some(Global))) {
        println("Hello")
      }
      success
    }

    "hooking global to log when error" in {

      import scalaguide.global.scalaglobal.views

      //#global-hooking-error
      import play.api._
      import play.api.mvc._
      import play.api.mvc.Results._
      import scala.concurrent.Future

      object Global extends GlobalSettings {

        override def onError(request: RequestHeader, ex: Throwable) = {
          Future.successful(InternalServerError(
            views.html.errorPage(ex)
          ))
        }

      }
      //#global-hooking-error

      val r = scalaguide.global.scalaglobal.Routes
      contentOf(rh=FakeRequest("GET", "/hello"),router = r,g=Global) === "hello"
    }


    "hooking global to handel not found request" in {

      import scalaguide.global.scalaglobal.views

      //#global-hooking-notfound
      import play.api._
      import play.api.mvc._
      import play.api.mvc.Results._
      import scala.concurrent.Future

      object Global extends GlobalSettings {

        override def onHandlerNotFound(request: RequestHeader) = {
          Future.successful(NotFound(
            views.html.notFoundPage(request.path)
          ))
        }

      }
      //#global-hooking-notfound

      contentOf(rh=FakeRequest("GET", "/hello"),g=Global) === "hello"
    }

    "hooking global to handel bad request parameter" in {

      import scalaguide.global.scalaglobal.views

      //#global-hooking-bad-request
      import play.api._
      import play.api.mvc._
      import play.api.mvc.Results._
      import scala.concurrent.Future

      object Global extends GlobalSettings {

        override def onBadRequest(request: RequestHeader, error: String) = {
          Future.successful(BadRequest("Bad Request: " + error))
        }

      }
      //#global-hooking-bad-request

      contentOf(rh=FakeRequest("GET", "/hello"),g=Global) === "hello"
    }



  }

  import play.api.mvc._
  import play.api.GlobalSettings

  def contentOf(rh: RequestHeader, router: Router.Routes = Routes,g:GlobalSettings) = running(
    FakeApplication(withGlobal=Some(g))
  )(contentAsString(router.routes(rh) match {
    case e: EssentialAction => e(rh).run
  }))

}

  object SourceDemo{

    def defineGlobal{
      //#global-define
      import play.api._

      object Global extends GlobalSettings {

      }
      //#global-define

    }
  }


package controllers {

import play.api.mvc.{Action, Controller}

object Application extends Controller{
  def index = Action(Ok("hello"))

  def bad = Action{
    throw new NullPointerException
    BadRequest
  }
}
}

}

