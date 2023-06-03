/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedKey
import play.api.libs.ws._
import play.api.mvc.ActionBuilder
import play.api.mvc.Handler
import play.api.mvc.Results
import play.api.test.PlaySpecification
import play.api.test.WsTestClient
import play.api.Application
import play.core.j.JavaHandler
import play.core.j.JavaHandlerComponents
import play.it.AkkaHttpIntegrationSpecification
import play.it.NettyIntegrationSpecification
import play.it.ServerIntegrationSpecification

class NettyJavaHttpHandlerSpec extends JavaHttpHandlerSpec with NettyIntegrationSpecification
class AkkaJavaHttpHandlerSpec  extends JavaHttpHandlerSpec with AkkaHttpIntegrationSpecification

trait JavaHttpHandlerSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {
  def handlerResponse[T](handler: Handler)(block: WSResponse => T): T = {
    val app: Application = GuiceApplicationBuilder()
      .routes {
        case _ => handler
      }
      .build()
    runningWithPort(TestServer(testServerPort, app)) { implicit port =>
      val response = await(wsUrl("/").get())
      block(response)
    }
  }

  val TestAttr = TypedKey[String]("testAttr")
  val javaHandler: JavaHandler = new JavaHandler {
    override def withComponents(components: JavaHandlerComponents): Handler = {
      ActionBuilder.ignoringBody { req => Results.Ok(req.attrs.get(TestAttr).toString) }
    }
  }

  "JavaCompatibleHttpHandler" should {
    "route requests to a JavaHandler's Action" in handlerResponse(javaHandler) { response =>
      response.body[String] must beEqualTo("None")
    }
    "route a modified request to a JavaHandler's Action" in handlerResponse(
      Handler.Stage.modifyRequest(req => req.addAttr(TestAttr, "Hello!"), javaHandler)
    ) { response => response.body[String] must beEqualTo("Some(Hello!)") }
  }
}
