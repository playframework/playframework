/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.global.scalaglobal {

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.GlobalSettings
import scala.concurrent.Future
import play.core.Router

@RunWith(classOf[JUnitRunner])
class ScalaInterceptorsSpec extends Specification with Controller {

  "A scala interceptor" should {

    "filter log request" in {

      //#filter-log
      import play.api.Logger
      import play.api.mvc._
      
      object AccessLoggingFilter extends Filter {
        def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
          val result = next(request)
          Logger.info(request + "\n\t => " + result)
          result
        }
      }

      object Global extends WithFilters(AccessLoggingFilter)
      //#filter-log

      contentOf(rh=FakeRequest("GET", "/hello"),g=Global) === "hello"
    }

    "filter authorize request" in {


      //#filter-authorize
      object AuthorizedFilter {
        def apply(actionNames: String*) = new AuthorizedFilter(actionNames)
      }

      class AuthorizedFilter(actionNames: Seq[String]) extends Filter {

        def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
          if(authorizationRequired(request)) {
            /* do the auth stuff here */
            println("auth required")
            next(request)
          }
          else next(request)
        }

        private def authorizationRequired(request: RequestHeader) = {
          val actionInvoked: String = request.tags.getOrElse(play.api.Routes.ROUTE_ACTION_METHOD, "")
          actionNames.contains(actionInvoked)
        }


      }


      object Global extends WithFilters(AuthorizedFilter("editProfile", "buy", "sell")) with GlobalSettings {}
      //#filter-authorize

      contentOf(rh=FakeRequest("GET", "/hello"),g=Global) === "hello"
    }

    "filter authorize request" in {


      //#onroute-request
      import play.api._
      import play.api.mvc._

      // Note: this is in the default package.
      object Global extends GlobalSettings {

        override def onRouteRequest(request: RequestHeader): Option[Handler] = {
          println("executed before every request:" + request.toString)
          super.onRouteRequest(request)
        }

      }
      //#onroute-request

      contentOf(rh=FakeRequest("GET", "/hello"),g=Global) === "hello"
    }


  }

  def contentOf(rh: RequestHeader, router: Router.Routes = Routes,g:GlobalSettings) = running(FakeApplication(withGlobal=Some(g)))(contentAsString(router.routes(rh) match {
    case e: EssentialAction => e(rh).run
  }))


}

}
