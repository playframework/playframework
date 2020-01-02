/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.Application
import play.api.Configuration
import play.api.libs.ws._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test._
import play.api.inject.guice._

class ServerSpec extends PlaySpecification {

  val httpServerTagRoutes: Application => PartialFunction[(String, String), Handler] = { app =>
    val Action = app.injector.instanceOf[DefaultActionBuilder]
    ({
      case ("GET", "/httpServerTag") =>
        Action { implicit request =>
          val httpServer = request.attrs.get(RequestAttrKey.Server).getOrElse("akka-http")
          Ok(httpServer)
        }
    })
  }

  "Functional tests" should {

    "support starting an Netty server in a test" in new WithServer(
      app = GuiceApplicationBuilder().appRoutes(httpServerTagRoutes).build()
    ) {
      val ws       = app.injector.instanceOf[WSClient]
      val response = await(ws.url("http://localhost:19001/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== "netty"
    }
  }
}
