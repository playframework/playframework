/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable._
import play.core.test._

object CookiesSpec extends Specification {

  "object Cookies#fromCookieHeader" should {

    "create new Cookies instance with cookies" in withApplication {
      val originalCookie = Cookie(name = "cookie", value = "value")

      val headerString = Cookies.encodeCookieHeader(Seq(originalCookie))
      val c = Cookies.fromCookieHeader(Some(headerString))

      c must beAnInstanceOf[Cookies]
    }

    "should create an empty Cookies instance with no header" in withApplication {
      val c = Cookies.fromCookieHeader(None)
      c must beAnInstanceOf[Cookies]
    }
  }

  "trait Cookies#get" should {
    val originalCookie = Cookie(name = "cookie", value = "value")
    def headerString = Cookies.encodeCookieHeader(Seq(originalCookie))
    def c: Cookies = Cookies.fromCookieHeader(Some(headerString))

    "get a cookie" in withApplication {
      c.get("cookie") must beSome[Cookie].which { cookie =>
        cookie.name must be_==("cookie")
      }
    }

    "return none if no cookie" in withApplication {
      c.get("no-cookie") must beNone
    }
  }

  "trait Cookies#apply" should {
    val originalCookie = Cookie(name = "cookie", value = "value")
    def headerString = Cookies.encodeCookieHeader(Seq(originalCookie))
    def c: Cookies = Cookies.fromCookieHeader(Some(headerString))

    "apply for a cookie" in withApplication {
      val cookie = c("cookie")
      cookie.name must be_==("cookie")
    }

    "throw error if no cookie" in withApplication {
      {
        c("no-cookie")
      }.must(throwA[RuntimeException](message = "Cookie doesn't exist"))
    }
  }

  "trait Cookies#traversable" should {
    val cookie1 = Cookie(name = "cookie1", value = "value2")
    val cookie2 = Cookie(name = "cookie2", value = "value2")

    "be empty for no cookies" in withApplication {
      val c = Cookies.fromCookieHeader(header = None)
      c must be empty
    }

    "contain elements for some cookies" in withApplication {
      val headerString = Cookies.encodeCookieHeader(Seq(cookie1, cookie2))
      println(headerString)
      val c: Cookies = Cookies.fromCookieHeader(Some(headerString))
      c must contain(allOf(cookie1, cookie2))
    }

    // technically the same as above
    "run a foreach for a cookie" in withApplication {
      val headerString = Cookies.encodeCookieHeader(Seq(cookie1))
      val c: Cookies = Cookies.fromCookieHeader(Some(headerString))

      var myCookie: Cookie = null
      c.foreach { cookie =>
        myCookie = cookie
      }
      myCookie must beEqualTo(cookie1)
    }
  }

}
