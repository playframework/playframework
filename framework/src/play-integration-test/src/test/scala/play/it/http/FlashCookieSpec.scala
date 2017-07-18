/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import com.typesafe.config.ConfigFactory
import play.api.http.{ FlashConfiguration, SecretConfiguration }
import play.api.libs.crypto.CookieSignerProvider
import play.api.{ BuiltInComponentsFromContext, Configuration, NoHttpFiltersComponents }
import play.api.test._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.ws.{ DefaultWSCookie, WSClient, WSCookie, WSResponse }
import play.api.routing.Router
import play.core.server.Server
import play.it._

class NettyFlashCookieSpec extends FlashCookieSpec with NettyIntegrationSpecification
class AkkaHttpFlashCookieSpec extends FlashCookieSpec with AkkaHttpIntegrationSpecification

trait FlashCookieSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {

  sequential

  def withClientAndServer[T](additionalConfiguration: Map[String, String] = Map.empty)(block: WSClient => T) = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {

        import play.api.routing.sird.{ GET => SirdGet, _ }
        import scala.collection.JavaConverters._

        override def configuration: Configuration = super.configuration ++ new Configuration(ConfigFactory.parseMap(additionalConfiguration.asJava))

        override def router: Router = Router.from {
          case SirdGet(p"/flash") => defaultActionBuilder {
            Redirect("/landing").flashing(
              "success" -> "found"
            )
          }
          case SirdGet(p"/set-cookie") => defaultActionBuilder {
            Ok.withCookies(Cookie("some-cookie", "some-value"))
          }
          case SirdGet(p"/landing") => defaultActionBuilder {
            Ok("ok")
          }
        }
      }.application
    } { implicit port =>
      withClient(block)
    }
  }

  lazy val flashCookieBaker: FlashCookieBaker = new DefaultFlashCookieBaker()

  def readFlashCookie(response: WSResponse): Option[WSCookie] =
    response.cookie(flashCookieBaker.COOKIE_NAME)

  "the flash cookie" should {
    "can be set for one request" in withClientAndServer() { ws =>
      val response = await(ws.url("/flash").withFollowRedirects(follow = false).get())
      response.status must equalTo(SEE_OTHER)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.maxAge must beNone
      }
    }

    "be removed after a redirect" in withClientAndServer() { ws =>
      val response = await(ws.url("/flash").get())
      response.status must equalTo(OK)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.value must ===("")
          cookie.maxAge must beSome(0L)
      }
    }

    "allow the setting of additional cookies when cleaned up" in withClientAndServer() { ws =>
      val response = await(ws.url("/flash").withFollowRedirects(false).get())
      val Some(flashCookie) = readFlashCookie(response)
      val response2 = await(ws.url("/set-cookie")
        .addCookies(DefaultWSCookie(flashCookie.name, flashCookie.value))
        .get())

      readFlashCookie(response2) must beSome.like {
        case cookie => cookie.value must ===("")
      }
      response2.cookie("some-cookie") must beSome.like {
        case cookie =>
          cookie.value must ===("some-value")
      }

    }

    "honor the configuration for play.http.flash.sameSite" in {
      "configured to null" in withClientAndServer(Map("play.http.flash.sameSite" -> null)) { ws =>
        val response = await(ws.url("/flash").withFollowRedirects(follow = false).get())
        response.status must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must beSome.which(!_.contains("SameSite"))
      }

      "configured to lax" in withClientAndServer(Map("play.http.flash.sameSite" -> "lax")) { ws =>
        val response = await(ws.url("/flash").withFollowRedirects(follow = false).get())
        response.status must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must beSome.which(_.contains("SameSite=Lax"))
      }

      "configured to strict" in withClientAndServer(Map("play.http.flash.sameSite" -> "strict")) { ws =>
        val response = await(ws.url("/flash").withFollowRedirects(follow = false).get())
        response.status must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must beSome.which(_.contains("SameSite=Strict"))
      }
    }

    "honor configuration for flash.secure" in {
      "configured to true" in Helpers.running(_.configure("play.http.flash.secure" -> true)) { _ =>
        val secretConfig = SecretConfiguration()
        val fcb: FlashCookieBaker = new DefaultFlashCookieBaker(
          FlashConfiguration(secure = true),
          secretConfig,
          new CookieSignerProvider(secretConfig).get
        )
        fcb.encodeAsCookie(Flash()).secure must beTrue
      }

      "configured to false" in Helpers.running(_.configure("play.http.flash.secure" -> false)) { _ =>
        val secretConfig = SecretConfiguration()
        val fcb: FlashCookieBaker = new DefaultFlashCookieBaker(
          FlashConfiguration(secure = false),
          secretConfig,
          new CookieSignerProvider(secretConfig).get
        )
        fcb.encodeAsCookie(Flash()).secure must beFalse
      }
    }

  }

}
