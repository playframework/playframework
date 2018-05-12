/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import com.typesafe.config.ConfigFactory
import play.api.{ BuiltInComponentsFromContext, Configuration, NoHttpFiltersComponents }
import play.api.http.{ SecretConfiguration, SessionConfiguration }
import play.api.libs.crypto.CookieSignerProvider
import play.api.test._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.ws.WSClient
import play.api.routing.Router
import play.core.server.Server
import play.it._

class NettySessionCookieSpec extends SessionCookieSpec with NettyIntegrationSpecification
class AkkaHttpSessionCookieSpec extends SessionCookieSpec with AkkaHttpIntegrationSpecification

trait SessionCookieSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {

  sequential

  def withClientAndServer[T](additionalConfiguration: Map[String, String] = Map.empty)(block: WSClient => T) = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {

        import play.api.routing.sird.{ GET => SirdGet, _ }
        import scala.collection.JavaConverters._

        override def configuration: Configuration = super.configuration ++ new Configuration(ConfigFactory.parseMap(additionalConfiguration.asJava))

        override def router: Router = Router.from {
          case SirdGet(p"/session") => defaultActionBuilder {
            Ok.withSession("session-key" -> "session-value")
          }
        }
      }.application
    } { implicit port =>
      withClient(block)
    }
  }

  "the session cookie" should {

    "honor configuration for play.http.session.sameSite" in {
      "configured to null" in withClientAndServer(Map("play.http.session.sameSite" -> null)) { ws =>
        val response = await(ws.url("/session").get())
        response.status must equalTo(OK)
        response.header(SET_COOKIE) must beSome.which(!_.contains("SameSite"))
      }

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
        val secretConfiguration = SecretConfiguration()
        val sessionCookieBaker: SessionCookieBaker = new DefaultSessionCookieBaker(
          SessionConfiguration(secure = true),
          secretConfiguration,
          new CookieSignerProvider(secretConfiguration).get
        )
        sessionCookieBaker.encodeAsCookie(Session()).secure must beTrue
      }

      "configured to false" in Helpers.running(_.configure("play.http.session.secure" -> false)) { _ =>
        val secretConfiguration = SecretConfiguration()
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
