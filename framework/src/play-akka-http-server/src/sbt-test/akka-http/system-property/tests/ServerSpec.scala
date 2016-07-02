/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import play.api.libs.ws._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._

class ServerSpec extends PlaySpecification {

  "Functional tests" should {

    val httpServerTagRoutes: PartialFunction[(String, String), Handler] = {
      case ("GET", "/httpServerTag") => Action { implicit request =>
        val httpServer = request.tags.get("HTTP_SERVER")
        Ok(httpServer.toString)
      }
    }

    "support starting an Akka HTTP server in a test" in new WithServer(
      app = FakeApplication(withRoutes = httpServerTagRoutes)) {

      val ws = app.injector.instanceOf[WSClient]
      val response = await(ws.url("http://localhost:19001/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== "Some(akka-http)"
    }

  }
}
