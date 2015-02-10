/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import org.specs2.mutable.Specification
import play.api.inject.BindingKey
import play.api.{ OptionalSourceMapper, Configuration, Mode, Environment }
import play.api.mvc.{ Results, RequestHeader }
import play.api.routing._
import play.core.test.{ FakeRequest, Fakes }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

object HttpErrorHandlerSpec extends Specification {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  "HttpErrorHandler" should {
    def sharedSpecs(errorHandler: HttpErrorHandler) = {
      "render a bad request" in {
        await(errorHandler.onClientError(FakeRequest(), 400)).header.status must_== 400
      }
      "render forbidden" in {
        await(errorHandler.onClientError(FakeRequest(), 403)).header.status must_== 403
      }
      "render not found" in {
        await(errorHandler.onClientError(FakeRequest(), 404)).header.status must_== 404
      }
      "render a generic client error" in {
        await(errorHandler.onClientError(FakeRequest(), 418)).header.status must_== 418
      }
      "refuse to render something that isn't a client error" in {
        await(errorHandler.onClientError(FakeRequest(), 500)).header.status must throwAn[IllegalArgumentException]
        await(errorHandler.onClientError(FakeRequest(), 399)).header.status must throwAn[IllegalArgumentException]
      }
      "render a server error" in {
        await(errorHandler.onServerError(FakeRequest(), new RuntimeException())).header.status must_== 500
      }
    }

    "work if a scala handler is defined" in {
      "in dev mode" in sharedSpecs(handler(classOf[DefaultHttpErrorHandler].getName, Mode.Dev))
      "in prod mode" in sharedSpecs(handler(classOf[DefaultHttpErrorHandler].getName, Mode.Prod))
    }

    "work if a java handler is defined" in {
      "in dev mode" in sharedSpecs(handler(classOf[play.http.DefaultHttpErrorHandler].getName, Mode.Dev))
      "in prod mode" in sharedSpecs(handler(classOf[play.http.DefaultHttpErrorHandler].getName, Mode.Prod))
    }

    "work with a custom scala handler" in {
      val result = handler(classOf[CustomScalaErrorHandler].getName, Mode.Prod).onClientError(FakeRequest(), 400)
      await(result).header.status must_== 200
    }

    "work with a custom java handler" in {
      val result = handler(classOf[CustomJavaErrorHandler].getName, Mode.Prod).onClientError(FakeRequest(), 400)
      await(result).header.status must_== 200
    }

  }

  def handler(handlerClass: String, mode: Mode.Mode) = {
    val config = Configuration.from(Map("play.http.errorHandler" -> handlerClass))
    val env = Environment.simple(mode = mode)
    Fakes.injectorFromBindings(HttpErrorHandler.bindingsFromConfiguration(env, config)
      ++ Seq(
        BindingKey(classOf[Router]).to(Router.empty),
        BindingKey(classOf[OptionalSourceMapper]).to(new OptionalSourceMapper(None)),
        BindingKey(classOf[Configuration]).to(config),
        BindingKey(classOf[Environment]).to(env)
      )).instanceOf[HttpErrorHandler]
  }

  class CustomScalaErrorHandler extends HttpErrorHandler {
    def onClientError(request: RequestHeader, statusCode: Int, message: String) =
      Future.successful(Results.Ok)
    def onServerError(request: RequestHeader, exception: Throwable) =
      Future.successful(Results.Ok)
  }

  class CustomJavaErrorHandler extends play.http.HttpErrorHandler {
    def onClientError(req: play.mvc.Http.RequestHeader, status: Int, msg: String) =
      play.libs.F.Promise.pure(play.mvc.Results.ok())
    def onServerError(req: play.mvc.Http.RequestHeader, exception: Throwable) =
      play.libs.F.Promise.pure(play.mvc.Results.ok())
  }

}
