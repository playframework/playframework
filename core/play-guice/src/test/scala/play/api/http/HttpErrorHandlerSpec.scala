/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.util.concurrent.CompletableFuture

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.http.HttpConfiguration.FileMimeTypesConfigurationProvider
import play.api.i18n._
import play.api.inject.ApplicationLifecycle
import play.api.inject.BindingKey
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.routing._
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.OptionalSourceMapper
import play.core.test.FakeRequest
import play.core.test.Fakes
import play.http
import play.i18n.Langs
import play.i18n.MessagesApi

class HttpErrorHandlerSpec extends Specification {
  import HttpErrorHandlerSpec._

  def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)

  implicit val system: ActorSystem        = ActorSystem()
  implicit val materializer: Materializer = Materializer.matFromSystem

  "HttpErrorHandler" should {
    def sharedSpecs(_eh: => HttpErrorHandler) = {
      lazy val errorHandler = _eh

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
        await(errorHandler.onServerError(FakeRequest(), new SimulateServerError)).header.status must_== 500
      }
    }

    def jsonResponsesSpecs(
        _eh: => HttpErrorHandler,
        isProdMode: Boolean
    )(implicit system: ActorSystem, materializer: Materializer) = {
      lazy val errorHandler = _eh

      def responseBody(result: Future[Result]): JsValue = Json.parse(await(await(result).body.consumeData).utf8String)

      "answer a JSON error message on bad request" in {
        val json = responseBody(errorHandler.onClientError(FakeRequest(), 400))
        (json \ "error" \ "requestId").get must beAnInstanceOf[JsNumber]
        (json \ "error" \ "message").get must beAnInstanceOf[JsString]
      }
      "answer a JSON error message on forbidden" in {
        val json = responseBody(errorHandler.onClientError(FakeRequest(), 403))
        (json \ "error" \ "requestId").get must beAnInstanceOf[JsNumber]
        (json \ "error" \ "message").get must beAnInstanceOf[JsString]
      }
      "answer a JSON error message on not found" in {
        val json = responseBody(errorHandler.onClientError(FakeRequest(), 404))
        (json \ "error" \ "requestId").get must beAnInstanceOf[JsNumber]
        (json \ "error" \ "message").get must beAnInstanceOf[JsString]
      }
      "answer a JSON error message on a generic client error" in {
        val json = responseBody(errorHandler.onClientError(FakeRequest(), 418))
        (json \ "error" \ "requestId").get must beAnInstanceOf[JsNumber]
        (json \ "error" \ "message").get must beAnInstanceOf[JsString]
      }
      "refuse to render something that isn't a client error" in {
        responseBody(errorHandler.onClientError(FakeRequest(), 500)) must throwAn[IllegalArgumentException]
        responseBody(errorHandler.onClientError(FakeRequest(), 399)) must throwAn[IllegalArgumentException]
      }
      "answer a JSON error message on a server error" in {
        val json                 = responseBody(errorHandler.onServerError(FakeRequest(), new RuntimeException()))
        val id                   = json \ "error" \ "id"
        val requestId            = json \ "error" \ "requestId"
        val exceptionTitle       = json \ "error" \ "exception" \ "title"
        val exceptionDescription = json \ "error" \ "exception" \ "description"
        val exceptionCause       = json \ "error" \ "exception" \ "stacktrace"

        if (isProdMode) {
          id.get must beAnInstanceOf[JsString]
          requestId.toOption must beNone
          exceptionTitle.toOption must beNone
          exceptionDescription.toOption must beNone
          exceptionCause.toOption must beNone
        } else {
          id.get must beAnInstanceOf[JsString]
          requestId.get must beAnInstanceOf[JsNumber]
          exceptionTitle.get must beAnInstanceOf[JsString]
          exceptionDescription.get must beAnInstanceOf[JsString]
          exceptionCause.get must beAnInstanceOf[JsArray]
          exceptionCause.get.as[List[String]].forall(!_.contains("""\n""")) must_== true
          exceptionCause.get.as[List[String]].forall(!_.contains("""\t""")) must_== true
        }
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

    "work if a scala JSON handler is defined" in {
      "in dev mode" in {
        def errorHandler = handler(classOf[JsonHttpErrorHandler].getName, Mode.Dev)
        sharedSpecs(errorHandler)
        jsonResponsesSpecs(errorHandler, isProdMode = false)
      }
      "in prod mode" in {
        def errorHandler = handler(classOf[JsonHttpErrorHandler].getName, Mode.Prod)
        sharedSpecs(errorHandler)
        jsonResponsesSpecs(errorHandler, isProdMode = true)
      }
    }

    "work if a java JSON handler is defined" in {
      "in dev mode" in {
        def errorHandler = handler(classOf[http.JsonHttpErrorHandler].getName, Mode.Dev)
        sharedSpecs(errorHandler)
        jsonResponsesSpecs(errorHandler, isProdMode = false)
      }
      "in prod mode" in {
        def errorHandler = handler(classOf[http.JsonHttpErrorHandler].getName, Mode.Prod)
        sharedSpecs(errorHandler)
        jsonResponsesSpecs(errorHandler, isProdMode = true)
      }
    }

    "work with a Scala HtmlOrJsonHttpErrorHandler" in {
      "a request when the client prefers JSON" in {
        def errorHandler = handler(classOf[HtmlOrJsonHttpErrorHandler].getName, Mode.Prod)
        "json response" in {
          val result = errorHandler.onClientError(FakeRequest().withHeaders("Accept" -> "application/json"), 400)
          await(result).body.contentType must beSome("application/json")
        }
        sharedSpecs(errorHandler)
      }
      "a request when the client prefers HTML" in {
        def errorHandler = handler(classOf[HtmlOrJsonHttpErrorHandler].getName, Mode.Prod)
        "html response" in {
          val result = errorHandler.onClientError(FakeRequest().withHeaders("Accept" -> "text/html"), 400)
          await(result).body.contentType must beSome("text/html; charset=utf-8")
        }
        sharedSpecs(errorHandler)
      }
    }

    "work with a Java HtmlOrJsonHttpErrorHandler" in {
      "a request when the client prefers JSON" in {
        def errorHandler = handler(classOf[play.http.HtmlOrJsonHttpErrorHandler].getName, Mode.Prod)
        "json response" in {
          val result = errorHandler.onClientError(FakeRequest().withHeaders("Accept" -> "application/json"), 400)
          await(result).body.contentType must beSome("application/json")
        }
        sharedSpecs(errorHandler)
      }
      "a request when the client prefers HTML" in {
        def errorHandler = handler(classOf[play.http.HtmlOrJsonHttpErrorHandler].getName, Mode.Prod)
        "html response" in {
          val result = errorHandler.onClientError(FakeRequest().withHeaders("Accept" -> "text/html"), 400)
          await(result).body.contentType must beSome("text/html; charset=utf-8")
        }
        sharedSpecs(errorHandler)
      }
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
      "play.http.secret.key"   -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"
    )
    val config            = ConfigFactory.parseMap(properties.asJava).withFallback(ConfigFactory.defaultReference())
    val configuration     = Configuration(config)
    val env               = Environment.simple(mode = mode)
    val jEnv              = env.asJava
    val httpConfiguration = HttpConfiguration.fromConfiguration(configuration, env)
    val langs             = new play.api.i18n.DefaultLangsProvider(configuration).get
    val messagesApi       = new DefaultMessagesApiProvider(env, configuration, langs, httpConfiguration).get
    val jLangs            = new play.i18n.Langs(langs)
    val jMessagesApi      = new play.i18n.MessagesApi(messagesApi)
    Fakes
      .injectorFromBindings(
        HttpErrorHandler.bindingsFromConfiguration(env, configuration)
          ++ Seq(
            BindingKey(classOf[ApplicationLifecycle]).to(new DefaultApplicationLifecycle()),
            BindingKey(classOf[Router]).to(Router.empty),
            BindingKey(classOf[OptionalSourceMapper]).to(new OptionalSourceMapper(None)),
            BindingKey(classOf[Configuration]).to(configuration),
            BindingKey(classOf[Config]).to(configuration.underlying),
            BindingKey(classOf[MessagesApi]).to(jMessagesApi),
            BindingKey(classOf[Langs]).to(jLangs),
            BindingKey(classOf[Environment]).to(env),
            BindingKey(classOf[play.Environment]).to(jEnv),
            BindingKey(classOf[HttpConfiguration]).to(httpConfiguration),
            BindingKey(classOf[FileMimeTypesConfiguration]).toProvider[FileMimeTypesConfigurationProvider],
            BindingKey(classOf[FileMimeTypes]).toProvider[DefaultFileMimeTypesProvider]
          )
      )
      .instanceOf[HttpErrorHandler]
  }
}

object HttpErrorHandlerSpec {
  final class SimulateServerError extends RuntimeException("simulate server error") with NoStackTrace
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
