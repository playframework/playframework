/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util

import okhttp3.{ CookieJar, HttpUrl }
import okhttp3.internal.http.HttpDate
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.it.test.{ ApplicationFactories, ApplicationFactory, EndpointIntegrationSpecification, OkHttpEndpointSupport }

import scala.collection.JavaConverters

class FlashCookieSpec extends PlaySpecification
  with EndpointIntegrationSpecification with OkHttpEndpointSupport with ApplicationFactories {

  /** Makes an app that we use while we're testing */
  def withFlashCookieApp(additionalConfiguration: Map[String, Any] = Map.empty): ApplicationFactory = {
    withConfigAndRouter(additionalConfiguration) { components =>
      import play.api.routing.sird.{ GET => SirdGet, _ }
      Router.from {
        case SirdGet(p"/flash") => components.defaultActionBuilder {
          Redirect("/landing").flashing(
            "success" -> "found"
          )
        }
        case SirdGet(p"/set-cookie") => components.defaultActionBuilder {
          Ok.withCookies(Cookie("some-cookie", "some-value"))
        }
        case SirdGet(p"/landing") => components.defaultActionBuilder {
          Ok("ok")
        }
      }
    }
  }

  /**
   * Handles the details of calling a [[play.it.test.ServerEndpoint]] with a cookie and
   * receiving the response and its cookies.
   */
  trait CookieEndpoint {
    def call(path: String, cookies: List[okhttp3.Cookie]): (okhttp3.Response, List[okhttp3.Cookie])
  }

  /**
   * Helper to add the `withAllCookieEndpoints` method to an `ApplicationFactory`.
   */
  implicit class CookieEndpointBaker(val appFactory: ApplicationFactory) {
    def withAllCookieEndpoints[A: AsResult](block: CookieEndpoint => A): Fragment = {
      appFactory.withAllOkHttpEndpoints { okEndpoint: OkHttpEndpoint =>
        block(new CookieEndpoint {
          import JavaConverters._
          def call(path: String, cookies: List[okhttp3.Cookie]): (okhttp3.Response, List[okhttp3.Cookie]) = {
            var responseCookies: List[okhttp3.Cookie] = null
            val cookieJar = new CookieJar {
              override def loadForRequest(url: HttpUrl): util.List[okhttp3.Cookie] = cookies.asJava
              override def saveFromResponse(url: HttpUrl, cookies: util.List[okhttp3.Cookie]): Unit = {
                assert(responseCookies == null, "This CookieJar only handles a single response")
                responseCookies = cookies.asScala.toList
              }
            }
            val client = okEndpoint.clientBuilder.followRedirects(false).cookieJar(cookieJar).build()
            val request = new okhttp3.Request.Builder().url(okEndpoint.endpoint.pathUrl(path)).build()
            val response = client.newCall(request).execute()
            val siteUrl = okhttp3.HttpUrl.parse(okEndpoint.endpoint.pathUrl("/"))
            assert(responseCookies != null, "The CookieJar should have received a response by now")
            (response, responseCookies)
          }
        })
      }
    }

  }

  lazy val flashCookieBaker: FlashCookieBaker = new DefaultFlashCookieBaker()

  /** Represents a session cookie in OkHttp */
  val SessionExpiry = HttpDate.MAX_DATE
  /** Represents any expired cookie in OkHttp */
  val PastExpiry = Long.MinValue

  "the flash cookie" should {

    "be set for first request and removed on next request" in withFlashCookieApp().withAllCookieEndpoints { fcep: CookieEndpoint =>
      // Make a request that returns a flash cookie
      val (response1, cookies1) = fcep.call("/flash", Nil)
      response1.code must equalTo(SEE_OTHER)
      val flashCookie1 = cookies1.find(_.name == flashCookieBaker.COOKIE_NAME)
      flashCookie1 must beSome.like {
        case cookie =>
          cookie.expiresAt must ===(SessionExpiry)
      }

      // Send back the flash cookie
      val redirectLocation = response1.header("Location")
      val (response2, cookies2) = fcep.call(redirectLocation, List(flashCookie1.get))

      // The returned flash cookie should now be cleared
      val flashCookie2 = cookies2.find(_.name == flashCookieBaker.COOKIE_NAME)
      flashCookie2 must beSome.like {
        case cookie =>
          cookie.value must ===("")
          cookie.expiresAt must ===(PastExpiry)
      }
    }

    "allow the setting of additional cookies when cleaned up" in withFlashCookieApp().withAllCookieEndpoints { fcep: CookieEndpoint =>
      // Get a flash cookie
      val (response1, cookies1) = fcep.call("/flash", Nil)
      response1.code must equalTo(SEE_OTHER)
      val flashCookie1 = cookies1.find(_.name == flashCookieBaker.COOKIE_NAME).get
      // Send request with flash cookie
      val (response2, cookies2) = fcep.call("/set-cookie", List(flashCookie1))
      val flashCookie2 = cookies2.find(_.name == flashCookieBaker.COOKIE_NAME)
      // Flash cookie should be cleared
      flashCookie2 must beSome.like {
        case cookie =>
          cookie.value must ===("")
          cookie.expiresAt must ===(PastExpiry)
      }
      // Another cookie should be set
      val someCookie2 = cookies2.find(_.name == "some-cookie")
      someCookie2 must beSome.like {
        case cookie => cookie.value must ===("some-value")
      }

    }

    "honor the configuration for play.http.flash.sameSite" in {

      "by not sending SameSite when configured to null" in withFlashCookieApp(Map("play.http.flash.sameSite" -> null)).withAllCookieEndpoints { fcep: CookieEndpoint =>
        val (response, cookies) = fcep.call("/flash", Nil)
        response.code must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must not contain ("SameSite")
      }

      "by sending SameSite=Lax when configured with 'lax'" in withFlashCookieApp(Map("play.http.flash.sameSite" -> "lax")).withAllCookieEndpoints { fcep: CookieEndpoint =>
        val (response, cookies) = fcep.call("/flash", Nil)
        response.code must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must contain("SameSite=Lax")
      }

      "by sending SameSite=Strict when configured with 'strict'" in withFlashCookieApp(Map("play.http.flash.sameSite" -> "lax")).withAllCookieEndpoints { fcep: CookieEndpoint =>
        val (response, cookies) = fcep.call("/flash", Nil)
        response.code must equalTo(SEE_OTHER)
        response.header(SET_COOKIE) must contain("SameSite=Lax")
      }

    }

    "honor configuration for flash.secure" in {

      "by making cookies secure when set to true" in withFlashCookieApp(Map("play.http.flash.secure" -> true)).withAllCookieEndpoints { fcep: CookieEndpoint =>
        val (response, cookies) = fcep.call("/flash", Nil)
        response.code must equalTo(SEE_OTHER)
        val cookie = cookies.find(_.name == flashCookieBaker.COOKIE_NAME)
        cookie must beSome.which(_.secure)
      }

      "by not making cookies secure when set to false" in withFlashCookieApp(Map("play.http.flash.secure" -> false)).withAllCookieEndpoints { fcep: CookieEndpoint =>
        val (response, cookies) = fcep.call("/flash", Nil)
        response.code must equalTo(SEE_OTHER)
        val cookie = cookies.find(_.name == flashCookieBaker.COOKIE_NAME)
        cookie must beSome.which(!_.secure)
      }

    }

  }

}
