/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.inject.guice._
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.Results._
import play.api.test._
import play.api.Application
import play.api.Configuration

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
      override def running() = {
        val ws       = app.injector.instanceOf[WSClient]
        val response = await(ws.url(s"http://localhost:$port/httpServerTag").get())
        response.status must equalTo(OK)
        response.body[String] must_== "netty"
      }
    }
  }
}
