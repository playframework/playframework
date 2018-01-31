/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedKey
import play.api.libs.ws.WSResponse
import play.api.mvc.{ ActionBuilder, Handler, Results }
import play.api.test.{ PlaySpecification, WsTestClient }
import play.core.j.{ JavaHandler, JavaHandlerComponents }
import play.it.{ AkkaHttpIntegrationSpecification, NettyIntegrationSpecification, ServerIntegrationSpecification }

class NettyJavaHttpHandlerSpec extends JavaHttpHandlerSpec with NettyIntegrationSpecification
class AkkaJavaHttpHandlerSpec extends JavaHttpHandlerSpec with AkkaHttpIntegrationSpecification

trait JavaHttpHandlerSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  def handlerResponse[T](handler: Handler)(block: WSResponse => T): T = {
    implicit val port = testServerPort
    val app: Application = GuiceApplicationBuilder().routes {
      case _ => handler
    }.build()
    running(TestServer(port, app)) {
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
      response.body must beEqualTo("None")
    }
    "route a modified request to a JavaHandler's Action" in handlerResponse(
      Handler.Stage.modifyRequest(req => req.addAttr(TestAttr, "Hello!"), javaHandler)
    ) { response =>
        response.body must beEqualTo("Some(Hello!)")
      }
  }
}
