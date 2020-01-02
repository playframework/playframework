/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import akka.util.ByteString
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.InMemoryBody
import play.api.libs.ws.WSBody
import play.api.libs.ws.WSResponse
import play.api.routing.Router
import play.api.test.PlaySpecification
import play.api.test.TestServer
import play.api.test.WsTestClient
import play.core.j.MappedJavaHandlerComponents
import play.http.ActionCreator
import play.http.DefaultActionCreator
import play.mvc.EssentialFilter
import play.mvc.Result
import play.mvc.Results
import play.mvc.Http._
import play.routing.{ Router => JRouter }

class GuiceJavaActionSpec extends JavaActionSpec {
  override def makeRequest[T](
      method: String,
      controller: MockController,
      configuration: Map[String, AnyRef] = Map.empty,
      body: WSBody = EmptyBody
  )(block: WSResponse => T): T = {
    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder()
      .configure(configuration)
      .routes {
        case _ => JAction(app, controller)
      }
      .build()

    running(TestServer(port, app)) {
      val response = await(wsUrl("/").withBody(body).execute(method))
      block(response)
    }
  }
}

class BuiltInComponentsJavaActionSpec extends JavaActionSpec {
  def context(initialSettings: Map[String, AnyRef]): play.ApplicationLoader.Context = {
    import scala.collection.JavaConverters._
    play.ApplicationLoader.create(play.Environment.simple(), initialSettings.asJava)
  }

  override def makeRequest[T](
      method: String,
      controller: MockController,
      configuration: Map[String, AnyRef] = Map.empty,
      body: WSBody = EmptyBody
  )(block: (WSResponse) => T): T = {
    implicit val port = testServerPort
    val components = new play.BuiltInComponentsFromContext(context(configuration)) {
      override def javaHandlerComponents(): MappedJavaHandlerComponents = {
        super
          .javaHandlerComponents()
      }

      override def router(): JRouter = {
        Router.from {
          case _ => JAction(application().asScala(), controller, javaHandlerComponents())
        }.asJava
      }

      override def httpFilters(): java.util.List[EssentialFilter] = java.util.Collections.emptyList()

      override def actionCreator(): ActionCreator = {
        configuration
          .get[Option[String]]("play.http.actionCreator")
          .map(Class.forName)
          .map(c => c.getDeclaredConstructor().newInstance().asInstanceOf[ActionCreator])
          .getOrElse(new DefaultActionCreator)
      }
    }

    running(TestServer(port, components.application().asScala())) {
      val response = await(wsUrl("/").withBody(body).execute(method))
      block(response)
    }
  }
}

trait JavaActionSpec extends PlaySpecification with WsTestClient {
  def makeRequest[T](
      method: String,
      controller: MockController,
      configuration: Map[String, AnyRef] = Map.empty,
      body: WSBody = EmptyBody
  )(block: WSResponse => T): T

  "POST request" should {
    "with no body should result in hasBody = false" in makeRequest(
      "POST",
      new MockController {
        override def action(request: Request): Result =
          Results.ok(
            s"hasBody: ${request.hasBody}, Content-Length: ${request.header(HeaderNames.CONTENT_LENGTH).orElse("")}"
          )
      }
    ) { response =>
      response.body must beEqualTo("hasBody: false, Content-Length: 0")
    }
    "with body should result in hasBody = true" in makeRequest(
      "POST",
      new MockController {
        override def action(request: Request): Result =
          Results.ok(
            s"hasBody: ${request.hasBody}, Content-Length: ${request.header(HeaderNames.CONTENT_LENGTH).orElse("")}"
          )
      },
      body = InMemoryBody(ByteString("a"))
    ) { response =>
      response.body must beEqualTo("hasBody: true, Content-Length: 1")
    }
  }
}
