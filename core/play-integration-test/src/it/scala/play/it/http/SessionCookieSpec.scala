/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import com.typesafe.config.ConfigFactory
import play.api.http.SecretConfiguration
import play.api.http.SessionConfiguration
import play.api.libs.crypto.CookieSignerProvider
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.test._
import play.api.BuiltInComponentsFromContext
import play.api.Configuration
import play.api.NoHttpFiltersComponents
import play.core.server.Server
import play.it._

class NettySessionCookieSpec    extends SessionCookieSpec with NettyIntegrationSpecification
class AkkaHttpSessionCookieSpec extends SessionCookieSpec with AkkaHttpIntegrationSpecification

trait SessionCookieSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {
  sequential

  def withClientAndServer[T](additionalConfiguration: Map[String, String] = Map.empty)(block: WSClient => T) = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
        import scala.jdk.CollectionConverters._

        import play.api.routing.sird.{ GET => SirdGet, _ }

        override def configuration: Configuration =
          Configuration(ConfigFactory.parseMap(additionalConfiguration.asJava)).withFallback(super.configuration)

        override def router: Router = Router.from {
          case SirdGet(p"/session") =>
            defaultActionBuilder {
              Ok.withSession("session-key" -> "session-value")
            }
        }
      }.application
    } { implicit port => withClient(block) }
  }

  "the session cookie" should {
    "honor configuration for play.http.session.sameSite" in {
      "configured to null" in withClientAndServer(Map("play.http.session.sameSite" -> null)) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome[String].which(!_.contains("SameSite"))
      }

      "configured to lax" in withClientAndServer(Map("play.http.session.sameSite" -> "lax")) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome[String].which(_.contains("SameSite=Lax"))
      }

      "configured to strict" in withClientAndServer(Map("play.http.session.sameSite" -> "strict")) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome[String].which(_.contains("SameSite=Strict"))
      }
    }

    "honor configuration for play.http.session.secure" in {
      "configured to true" in Helpers.running(_.configure("play.http.session.secure" -> true)) { _ =>
        val secretConfiguration =
          SecretConfiguration(secret = "vQU@MgnjTohP?w>jpu?X0oqvmz21o[AHP;/rPj?CB><YMFcl?xXfq]6o>1QuNcXU")
        val sessionCookieBaker: SessionCookieBaker = new DefaultSessionCookieBaker(
          SessionConfiguration(secure = true),
          secretConfiguration,
          new CookieSignerProvider(secretConfiguration).get
        )
        sessionCookieBaker.encodeAsCookie(Session()).secure must beTrue
      }

      "configured to false" in Helpers.running(_.configure("play.http.session.secure" -> false)) { _ =>
        val secretConfiguration =
          SecretConfiguration(secret = "vQU@MgnjTohP?w>jpu?X0oqvmz21o[AHP;/rPj?CB><YMFcl?xXfq]6o>1QuNcXU")
        val sessionCookieBaker: SessionCookieBaker = new DefaultSessionCookieBaker(
          SessionConfiguration(secure = false),
          secretConfiguration,
          new CookieSignerProvider(secretConfiguration).get
        )
        sessionCookieBaker.encodeAsCookie(Session()).secure must beFalse
      }
    }
  }
}
