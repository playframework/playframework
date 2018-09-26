/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.libs.ws._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test._
import play.api.inject.guice._

class ServerSpec extends PlaySpecification {

  val httpServerTagRoutes: PartialFunction[(String, String), Handler] = {
    case ("GET", "/httpServerTag") => Action { implicit request =>
      val httpServer = request.attrs.get(RequestAttrKey.Server)
      Ok(httpServer.toString)
    }
  }

  "Functional tests" should {

    "support starting an Akka HTTP server in a test" in new WithServer(
      app = GuiceApplicationBuilder().routes(httpServerTagRoutes).build()) {
      val ws = app.injector.instanceOf[WSClient]
      val response = await(ws.url("http://localhost:19001/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== "Some(akka-http)"
    }
  }
}
