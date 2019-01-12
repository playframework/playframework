/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test._

class NettyServerIntegrationSpecificationSpec extends ServerIntegrationSpecificationSpec with NettyIntegrationSpecification {
  override def expectedServerTag = Some("netty")
}
class AkkaHttpServerIntegrationSpecificationSpec extends ServerIntegrationSpecificationSpec with AkkaHttpIntegrationSpecification {
  override def expectedServerTag = None
}

/**
 * Tests that the ServerIntegrationSpecification, a helper for testing with different
 * server backends, works properly.
 */
trait ServerIntegrationSpecificationSpec extends PlaySpecification
  with WsTestClient with ServerIntegrationSpecification {

  def expectedServerTag: Option[String]

  "ServerIntegrationSpecification" should {

    val httpServerTagRoutes: PartialFunction[(String, String), Handler] = {
      case ("GET", "/httpServerTag") => ActionBuilder.ignoringBody { implicit request: RequestHeader =>
        val httpServer = request.attrs.get(RequestAttrKey.Server)
        Ok(httpServer.toString)
      }
    }

    "run the right HTTP server when using TestServer constructor" in {
      running(TestServer(testServerPort, GuiceApplicationBuilder().routes(httpServerTagRoutes).build())) {
        val plainRequest = wsUrl("/httpServerTag")(testServerPort)
        val responseFuture = plainRequest.get()
        val response = await(responseFuture)
        response.status must_== 200
        response.body must_== expectedServerTag.toString
      }
    }

    "run the right server when using WithServer trait" in new WithServer(
      app = GuiceApplicationBuilder().routes(httpServerTagRoutes).build()) {
      val response = await(wsUrl("/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== expectedServerTag.toString
    }

  }
}
