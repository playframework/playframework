/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.mvc.{ Cookie, Flash, Action }
import play.api.mvc.Results._
import play.api.libs.ws.{ WSClient, WSCookie, WSResponse }
import play.core.server.Server
import play.it._

object NettyFlashCookieSpec extends FlashCookieSpec with NettyIntegrationSpecification
object AkkaHttpFlashCookieSpec extends FlashCookieSpec with AkkaHttpIntegrationSpecification

trait FlashCookieSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {

  sequential

  def appWithRedirect = GuiceApplicationBuilder().routes {
    case ("GET", "/flash") =>
      Action {
        Redirect("/landing").flashing(
          "success" -> "found"
        )
      }
    case ("GET", "/set-cookie") =>
      Action {
        Ok.withCookies(Cookie("some-cookie", "some-value"))
      }
    case ("GET", "/landing") =>
      Action {
        Ok("ok")
      }
  }.build()

  def withClientAndServer[T](block: WSClient => T) = {
    val app = appWithRedirect
    import app.materializer
    Server.withApplication(app) { implicit port =>
      withClient(block)
    }
  }

  def readFlashCookie(response: WSResponse): Option[WSCookie] =
    response.cookies.find(_.name.exists(_ == Flash.COOKIE_NAME))

  "the flash cookie" should {
    "can be set for one request" in withClientAndServer { ws =>
      val response = await(ws.url("/flash").withFollowRedirects(follow = false).get())
      response.status must equalTo(SEE_OTHER)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.maxAge must beNone
      }
    }

    "be removed after a redirect" in withClientAndServer { ws =>
      val response = await(ws.url("/flash").get())
      response.status must equalTo(OK)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.value must beNone
          cookie.maxAge must beSome(0L)
      }
    }

    "allow the setting of additional cookies when cleaned up" in withClientAndServer { ws =>
      val response = await(ws.url("/flash").withFollowRedirects(false).get())
      val Some(flashCookie) = readFlashCookie(response)
      val response2 = await(ws.url("/set-cookie")
        .withHeaders("Cookie" -> s"${flashCookie.name.get}=${flashCookie.value.get}")
        .get())

      readFlashCookie(response2) must beSome.like {
        case cookie => cookie.value must beNone
      }
      response2.cookie("some-cookie") must beSome.like {
        case cookie =>
          cookie.value must beSome("some-value")
      }

    }

    "honor configuration for flash.secure" in Helpers.running(_.configure("play.http.flash.secure" -> true)) { _ =>
      Flash.encodeAsCookie(Flash()).secure must beTrue
    }
  }

}
