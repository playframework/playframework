/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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

  "Cookie" should {

    val encoder = play.core.netty.utils.ServerCookieEncoder.STRICT

    "properly encode ! character" in {
      val output = encoder.encode("TestCookie", "!")
      output must be_==("TestCookie=!")
    }

    // see #4460 for the gory details
    "properly encode all special characters" in {
      val output = encoder.encode("TestCookie", "!#$%&'()*+-./:<=>?@[]^_`{|}~")
      output must be_==("TestCookie=!#$%&'()*+-./:<=>?@[]^_`{|}~")
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

  "object Cookies#decodeSetCookieHeader" should {
    "parse empty string without exception " in {
      val decoded = Cookies.decodeSetCookieHeader("")
      decoded must be empty
    }
  }

  "merging cookies" should {
    "replace old cookies with new cookies of the same name" in {
      val originalRequest = FakeRequest().withCookies(Cookie("foo", "fooValue1"), Cookie("bar", "barValue2"))
      val requestWithMoreCookies = originalRequest.withCookies(Cookie("foo", "fooValue2"), Cookie("baz", "bazValue"))
      val cookies = requestWithMoreCookies.cookies
      cookies.toSet must_== Set(
        Cookie("foo", "fooValue2"),
        Cookie("bar", "barValue2"),
        Cookie("baz", "bazValue")
      )
    }
    "return one cookie for each name" in {
      val cookies = FakeRequest().withCookies(
        Cookie("foo", "foo1"), Cookie("foo", "foo2"), Cookie("bar", "bar"), Cookie("baz", "baz")
      ).cookies
      cookies.toSet must_== Set(
        Cookie("foo", "foo2"),
        Cookie("bar", "bar"),
        Cookie("baz", "baz")
      )
    }
  }
}
