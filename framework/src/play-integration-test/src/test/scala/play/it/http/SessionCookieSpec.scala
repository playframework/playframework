/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.ws.WSClient
import play.core.server.Server
import play.it._

class NettySessionCookieSpec extends SessionCookieSpec with NettyIntegrationSpecification
class AkkaHttpSessionCookieSpec extends SessionCookieSpec with AkkaHttpIntegrationSpecification

trait SessionCookieSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {

  sequential

  def appWithActions(additionalConfiguration: Map[String, String]) = GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .appRoutes(app => {
      val Action = app.injector.instanceOf[DefaultActionBuilder]
      ({
        case ("GET", "/session") =>
          Action {
            Ok.withSession("session-key" -> "session-value")
          }
      })
    }).build()

  def withClientAndServer[T](additionalConfiguration: Map[String, String] = Map.empty)(block: WSClient => T) = {
    val app = appWithActions(additionalConfiguration)
    import app.materializer
    Server.withApplication(app) { implicit port =>
      withClient(block)
    }
  }

  "the session cookie" should {

    "honor configuration for play.http.session.sameSite" in {
      "configured to lax" in withClientAndServer(Map("play.http.session.sameSite" -> "lax")) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome.which(_.contains("SameSite=Lax"))
      }

      "configured to strict" in withClientAndServer(Map("play.http.session.sameSite" -> "strict")) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome.which(_.contains("SameSite=Strict"))
      }
    }

    "honor configuration for play.http.session.secure" in {
      "configured to true" in Helpers.running(_.configure("play.http.session.secure" -> true)) { _ =>
        Session.encodeAsCookie(Session()).secure must beTrue
      }

      "configured to false" in Helpers.running(_.configure("play.http.session.secure" -> false)) { _ =>
        Session.encodeAsCookie(Session()).secure must beFalse
      }
    }

  }

}
