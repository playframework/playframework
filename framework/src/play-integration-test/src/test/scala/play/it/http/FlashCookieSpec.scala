package play.it.http

import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.{Flash, Action}
import play.api.mvc.Results._
import play.api.libs.ws.{Cookie, Response, WS}
import play.api.Logger

object FlashCookieSpec extends PlaySpecification {

  sequential

  val appWithRedirect = FakeApplication(withRoutes = {
    case ("GET", "/flash") =>
      Action {
        Redirect("/landing").flashing(
          "success" -> "found"
        )
      }
    case ("GET", "/landing") =>
      Action {
        Ok("ok")
      }
  })

  def readFlashCookie(response: Response): Option[Cookie] =
    response.cookies.find(_.name.exists(_ == Flash.COOKIE_NAME))

  "the flash cookie" should {
    "can be set for one request" in new WithServer(app = appWithRedirect, port = 3333) {
      val response = await(WS.url("http://localhost:3333/flash").withFollowRedirects(false).get())
      response.status must equalTo(SEE_OTHER)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.expires must beNone
          cookie.maxAge must beNone
      }
    }

    "be removed after a redirect" in new WithServer(app = appWithRedirect, port = 3333) {
      val response = await(WS.url("http://localhost:3333/flash").get())
      response.status must equalTo(OK)
      val flashCookie = readFlashCookie(response)
      flashCookie must beSome.like {
        case cookie =>
          cookie.expires must beSome.like {
            case expires => expires must be lessThan System.currentTimeMillis()
          }
          // Netty sends a negative max age, async-http-client treats that as 0
          cookie.maxAge must beSome(0)
      }
    }
  }


}
