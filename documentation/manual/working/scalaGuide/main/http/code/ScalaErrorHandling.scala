/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.errorhandling

import scala.reflect.ClassTag

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.DefaultActionBuilder
import play.api.test._

class ScalaErrorHandling extends PlaySpecification with WsTestClient {
  def fakeApp[A](implicit ct: ClassTag[A]) = {
    GuiceApplicationBuilder()
      .configure("play.http.errorHandler" -> ct.runtimeClass.getName)
      .appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        ({
          case (_, "/error") => Action(_ => throw new RuntimeException("foo"))
        })
      }
      .build()
  }

  "scala error handling" should {
    "allow providing a custom error handler" in (new WithServer(fakeApp[root.ErrorHandler]) {
      override def running() = {
        import play.api.libs.ws.DefaultBodyReadables.readableAsString
        await(wsUrl("/error").get()).body must_== "A server error occurred: foo"
      }
    })()

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
  import javax.inject.Singleton

  import scala.concurrent._

  import play.api.http.HttpErrorHandler
  import play.api.mvc._
  import play.api.mvc.Results._

  @Singleton
  class ErrorHandler extends HttpErrorHandler {
    def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
      Future.successful(
        Status(statusCode)("A client error occurred: " + message)
      )
    }

    def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
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

  import scala.concurrent._

  import play.api._
  import play.api.http.DefaultHttpErrorHandler
  import play.api.mvc._
  import play.api.mvc.Results._
  import play.api.routing.Router

  @Singleton
  class ErrorHandler @Inject() (
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

package custom {
//#custom-media-type
  import javax.inject._

  import play.api.http._

  class MyHttpErrorHandler @Inject() (
      jsonHandler: JsonHttpErrorHandler,
      htmlHandler: DefaultHttpErrorHandler,
      textHandler: MyTextHttpErrorHandler
  ) extends PreferredMediaTypeHttpErrorHandler(
        "application/json" -> jsonHandler,
        "text/html"        -> htmlHandler,
        "text/plain"       -> textHandler
      )
//#custom-media-type

  import scala.concurrent._

  import play.api.mvc._

  class MyTextHttpErrorHandler extends HttpErrorHandler {
    def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = ???
    def onServerError(request: RequestHeader, exception: Throwable): Future[Result]             = ???
  }
}
