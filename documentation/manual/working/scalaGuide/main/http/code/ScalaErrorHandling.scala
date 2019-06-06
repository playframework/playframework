/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.http.errorhandling

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Action
import play.api.test._

import scala.reflect.ClassTag

class ScalaErrorHandling extends PlaySpecification with WsTestClient {

  def fakeApp[A](implicit ct: ClassTag[A]) = {
    GuiceApplicationBuilder()
      .configure("play.http.errorHandler" -> ct.runtimeClass.getName)
      .routes {
        case (_, "/error") => Action(_ => throw new RuntimeException("foo"))
      }
      .build()
  }

  "scala error handling" should {
    "allow providing a custom error handler" in new WithServer(fakeApp[root.ErrorHandler]) {
      await(wsUrl("/error").get()).body must_== "A server error occurred: foo"
    }

    "allow extending the default error handler" in {
      import play.api._
      import play.api.routing._
      import javax.inject.Provider
      def errorHandler(mode: Mode) = new default.ErrorHandler(
        Environment.simple(mode = mode),
        Configuration.empty,
        new OptionalSourceMapper(None),
        new Provider[Router] { def get = Router.empty }
      )
      def errorContent(mode: Mode) =
        contentAsString(errorHandler(mode).onServerError(FakeRequest(), new RuntimeException("foo")))

      errorContent(Mode.Prod) must startWith("A server error occurred: ")
      (errorContent(Mode.Dev) must not).startWith("A server error occurred: ")
    }

  }

}

package root {
//#root
  import play.api.http.HttpErrorHandler
  import play.api.mvc._
  import play.api.mvc.Results._
  import scala.concurrent._
  import javax.inject.Singleton

  @Singleton
  class ErrorHandler extends HttpErrorHandler {

    def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
      Future.successful(
        Status(statusCode)("A client error occurred: " + message)
      )
    }

    def onServerError(request: RequestHeader, exception: Throwable) = {
      Future.successful(
        InternalServerError("A server error occurred: " + exception.getMessage)
      )
    }
  }
//#root
}

package default {
//#default
  import javax.inject._

  import play.api.http.DefaultHttpErrorHandler
  import play.api._
  import play.api.mvc._
  import play.api.mvc.Results._
  import play.api.routing.Router
  import scala.concurrent._

  @Singleton
  class ErrorHandler @Inject()(
      env: Environment,
      config: Configuration,
      sourceMapper: OptionalSourceMapper,
      router: Provider[Router]
  ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

    override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
      Future.successful(
        InternalServerError("A server error occurred: " + exception.getMessage)
      )
    }

    override def onForbidden(request: RequestHeader, message: String) = {
      Future.successful(
        Forbidden("You're not allowed to access this resource.")
      )
    }
  }
//#default
}
