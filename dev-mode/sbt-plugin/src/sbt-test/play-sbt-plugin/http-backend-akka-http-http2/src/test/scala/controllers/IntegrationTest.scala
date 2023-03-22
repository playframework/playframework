/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import play.api.http.HttpProtocol
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.test._
import play.api.test.PlaySpecification

class IntegrationTest extends ForServer with PlaySpecification with ApplicationFactories {

  protected def applicationFactory: ApplicationFactory = withGuiceApp(GuiceApplicationBuilder())

  def wsUrl(path: String)(implicit running: RunningServer): WSRequest = {
    val ws  = running.app.injector.instanceOf[WSClient]
    val url = running.endpoints.httpEndpoint.get.pathUrl(path)
    ws.url(url).withVirtualHost("127.0.0.1")
  }

  "Integration test" should {

    "use the controller successfully" >> { implicit rs: RunningServer =>
      val result = await(wsUrl("/").get())
      result.status must ===(200)
    }

    "use the user-configured HTTP backend during test" >> { implicit rs: RunningServer =>
      val result = await(wsUrl("/").get())
      // This assertion indirectly checks the HTTP backend used during tests is that configured
      // by the user on `build.sbt`.
      result.header("Server") must ===(Some("AkkaHTTP Server Http2"))
    }

    "use the user-configured HTTP transports during test" >> { implicit rs: RunningServer =>
      rs.endpoints.endpoints.filter(_.protocols.contains(HttpProtocol.HTTP_2_0)) must not be Nil
    }

  }
}
