/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import java.util.concurrent.CompletableFuture

import com.typesafe.config.Config
import org.specs2.mutable.Specification
import play.api.inject.BindingKey
import play.api.mvc.{ RequestHeader, Result, Results }
import play.api.routing._
import play.api.{ Configuration, Environment, Mode, OptionalSourceMapper }
import play.core.test.{ FakeRequest, Fakes }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

object HttpErrorHandlerSpec extends Specification {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  "HttpErrorHandler" should {
    def sharedSpecs(errorHandler: HttpErrorHandler) = {
      "render a bad request" in {
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 400))).header.status must_== 400
      }
      "render forbidden" in {
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 403))).header.status must_== 403
      }
      "render not found" in {
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 404))).header.status must_== 404
      }
      "render a generic client error" in {
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 418))).header.status must_== 418
      }
      "refuse to render something that isn't a client error" in {
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 500))).header.status must throwAn[IllegalArgumentException]
        await(errorHandler.onError(HttpError.fromString(FakeRequest(), 399))).header.status must throwAn[IllegalArgumentException]
      }
      "render a server error" in {
        await(errorHandler.onError(HttpServerError(FakeRequest(), new RuntimeException()))).header.status must_== 500
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
      val result = handler(classOf[CustomScalaErrorHandler].getName, Mode.Prod).onError(HttpError.fromString(FakeRequest(), 400))
      await(result).header.status must_== 200
    }

    "work with a custom java handler" in {
      val result = handler(classOf[CustomJavaErrorHandler].getName, Mode.Prod).onError(HttpError.fromString(FakeRequest(), 400))
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
        BindingKey(classOf[Config]).to(config.underlying),
        BindingKey(classOf[Environment]).to(env)
      )).instanceOf[HttpErrorHandler]
  }

  class CustomScalaErrorHandler extends HttpErrorHandler {
    override def onError(error: HttpError[_]): Future[Result] =
      Future.successful(Results.Ok)
  }

  class CustomJavaErrorHandler extends play.http.HttpErrorHandler {
    override def onError(error: play.http.HttpError[_]) =
      CompletableFuture.completedFuture(play.mvc.Results.ok())
  }

}
