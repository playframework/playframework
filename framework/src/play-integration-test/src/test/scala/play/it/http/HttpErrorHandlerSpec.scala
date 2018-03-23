/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import java.util.concurrent.CompletableFuture

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.{ PlaySpecification, TestServer, WsTestClient }
import play.mvc.{ Http, Result, Results }

class HttpErrorHandlerSpec extends PlaySpecification with WsTestClient {
  "A Java http error handler" should {
    "be able to access the context args previously set within a failed action method" in makeRequest(new MockController {
      override def action: Result = {
        Http.Context.current().args().put("foobar", "aBc_xYz")
        throw new RuntimeException("booom!")
      }
    }, Map("play.http.errorHandler" -> classOf[CustomJavaErrorHandler].getName)) { response =>
      response.body must beEqualTo("Hello from http error handler onServerError: aBc_xYz")
    }
  }

  def makeRequest[T](controller: MockController, configuration: Map[String, AnyRef] = Map.empty)(block: WSResponse => T): T = {
    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder().configure(configuration).routes {
      case _ => JAction(app, controller)
    }.build()

    running(TestServer(port, app)) {
      val response = await(wsUrl("/").get())
      block(response)
    }
  }
}

class CustomJavaErrorHandler extends play.http.HttpErrorHandler {
  override def onServerError(req: play.mvc.Http.RequestHeader, exception: Throwable) =
    CompletableFuture.completedFuture(play.mvc.Results.ok("Hello from http error handler onServerError: " + Http.Context.current().args().get("foobar")))
  override def onClientError(req: play.mvc.Http.RequestHeader, status: Int, msg: String) =
    CompletableFuture.completedFuture(play.mvc.Results.ok("I will not be called"))
}
