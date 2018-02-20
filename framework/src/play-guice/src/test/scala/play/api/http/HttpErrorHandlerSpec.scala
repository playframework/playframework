/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.util.concurrent.CompletableFuture

import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration.FileMimeTypesConfigurationProvider
import play.api.i18n._
import play.api.inject.BindingKey
import play.api.mvc.{ RequestHeader, Results }
import play.api.routing._
import play.api.{ Configuration, Environment, Mode, OptionalSourceMapper }
import play.core.test.{ FakeRequest, Fakes }
import play.i18n.{ Langs, MessagesApi }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

import scala.collection.JavaConverters._

class HttpErrorHandlerSpec extends Specification {

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

  def handler(handlerClass: String, mode: Mode): HttpErrorHandler = {
    val properties = Map(
      "play.http.errorHandler" -> handlerClass,
      "play.http.secret.key" -> "mysecret"
    )
    val config = ConfigFactory.parseMap(properties.asJava).withFallback(ConfigFactory.defaultReference())
    val configuration = Configuration(config)
    val env = Environment.simple(mode = mode)
    val httpConfiguration = HttpConfiguration.fromConfiguration(configuration, env)
    val langs = new play.api.i18n.DefaultLangsProvider(configuration).get
    val messagesApi = new DefaultMessagesApiProvider(env, configuration, langs, httpConfiguration).get
    val jLangs = new play.i18n.Langs(langs)
    val jMessagesApi = new play.i18n.MessagesApi(messagesApi)
    Fakes.injectorFromBindings(HttpErrorHandler.bindingsFromConfiguration(env, configuration)
      ++ Seq(
        BindingKey(classOf[Router]).to(Router.empty),
        BindingKey(classOf[OptionalSourceMapper]).to(new OptionalSourceMapper(None)),
        BindingKey(classOf[Configuration]).to(configuration),
        BindingKey(classOf[Config]).to(configuration.underlying),
        BindingKey(classOf[MessagesApi]).to(jMessagesApi),
        BindingKey(classOf[Langs]).to(jLangs),
        BindingKey(classOf[Environment]).to(env),
        BindingKey(classOf[HttpConfiguration]).to(httpConfiguration),
        BindingKey(classOf[FileMimeTypesConfiguration]).toProvider[FileMimeTypesConfigurationProvider],
        BindingKey(classOf[FileMimeTypes]).toProvider[DefaultFileMimeTypesProvider]
      )).instanceOf[HttpErrorHandler]
  }

}

class CustomScalaErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String) =
    Future.successful(Results.Ok)
  def onServerError(request: RequestHeader, exception: Throwable) =
    Future.successful(Results.Ok)
}

class CustomJavaErrorHandler extends play.http.HttpErrorHandler {
  def onClientError(req: play.mvc.Http.RequestHeader, status: Int, msg: String) =
    CompletableFuture.completedFuture(play.mvc.Results.ok())
  def onServerError(req: play.mvc.Http.RequestHeader, exception: Throwable) =
    CompletableFuture.completedFuture(play.mvc.Results.ok())
}
