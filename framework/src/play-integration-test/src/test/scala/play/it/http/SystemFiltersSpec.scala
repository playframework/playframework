/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.i18n._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.it.{ AkkaHttpIntegrationSpecification, NettyIntegrationSpecification, ServerIntegrationSpecification }

import scala.concurrent.Await
import scala.reflect.ClassTag

class NettySystemFiltersSpec extends SystemFiltersSpec with NettyIntegrationSpecification
class AkkaHttpSystemFiltersSpec extends SystemFiltersSpec with AkkaHttpIntegrationSpecification

/**
 * Tests that we can add request attributes.
 */
trait SystemFiltersSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {
  import scala.concurrent.duration._
  import SystemFiltersSpec._

  sequential

  def withServer[T](builder: GuiceApplicationBuilder, action: Action[_])(block: Port => T) = {
    val port = testServerPort
    val app = builder.routes {
      case _ => action
    }.build()
    running(TestServer(port, app)) {
      block(port)
    }
  }

  def messagesAction[T: ClassTag] = Action { request =>
    val value = request.attr(play.api.i18n.RequestAttributes.MessagesApiAttr)
    value match {
      case instance: T =>
        Results.Ok("success")
      case other =>
        Results.NotFound(s"failure: other = $other")
    }
  }

  "get at messages api through request attribute" in withServer(GuiceApplicationBuilder(), messagesAction[DefaultMessagesApi]) { port =>
    WsTestClient.withClient { client =>
      val response = Await.result(client.url(s"http://localhost:${port}").get(), 10.seconds)
      response.body must_== "success"
    }
  }

  "get at my messages api through override" in withServer(GuiceApplicationBuilder()
    .disable[play.api.i18n.I18nModule]
    .bindings(new MyI18nModule),
    messagesAction[MyMessagesApi]) { port =>
      WsTestClient.withClient { client =>
        val response = Await.result(client.url(s"http://localhost:${port}").get(), 10.seconds)
        response.body must_== "success"
      }
    }
}

object SystemFiltersSpec {
  import play.api.inject.Module
  import play.api.{ Configuration, Environment }

  class MyI18nModule extends Module {
    def bindings(env: Environment, conf: Configuration) = Seq(
      bind[Langs].toProvider[DefaultLangsProvider],
      bind[MessagesApi].to[MyMessagesApi]
    )
  }

  class MyMessagesApi extends MessagesApi {
    override def messages: Map[String, Map[String, String]] = ???
    override def preferred(candidates: Seq[Lang]): Messages = ???
    override def preferred(request: play.api.mvc.RequestHeader): Messages = ???
    override def preferred(request: play.mvc.Http.RequestHeader): Messages = ???
    override def setLang(result: Result, lang: Lang): Result = ???
    override def clearLang(result: Result): Result = ???
    override def apply(key: String, args: Any*)(implicit lang: Lang): String = ???
    override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = ???
    override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = ???
    override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = ???
    override def langCookieName: String = ???
    override def langCookieSecure: Boolean = ???
    override def langCookieHttpOnly: Boolean = ???
  }

}

