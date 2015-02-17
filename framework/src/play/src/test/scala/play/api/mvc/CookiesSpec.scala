/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable._

object CookiesSpec extends Specification {

  "object Cookies#apply" should {

    "create new Cookies instance with cookies" in {
      val originalCookie = Cookie(name = "cookie", value = "value")

      val headerString = Cookies.encode(Seq(originalCookie))
      val c = Cookies(header = Some(headerString))

      c must beAnInstanceOf[Cookies]
    }

    "should create an empty Cookies instance with no header" in {
      val c = Cookies(header = None)
      c must beAnInstanceOf[Cookies]
    }
  }

  "trait Cookies#get" should {
    val originalCookie = Cookie(name = "cookie", value = "value")
    val headerString = Cookies.encode(Seq(originalCookie))
    val c: Cookies = Cookies(header = Some(headerString))

    "get a cookie" in {
      c.get("cookie") must beSome[Cookie].which { cookie =>
        cookie.name must be_==("cookie")
      }
    }

    "return none if no cookie" in {
      c.get("no-cookie") must beNone
    }
  }

  "trait Cookies#apply" should {
    val originalCookie = Cookie(name = "cookie", value = "value")
    val headerString = Cookies.encode(Seq(originalCookie))
    val c: Cookies = Cookies(header = Some(headerString))

    "apply for a cookie" in {
      val cookie = c("cookie")
      cookie.name must be_==("cookie")
    }

    "throw error if no cookie" in {
      {
        c("no-cookie")
      }.must(throwA[RuntimeException](message = "Cookie doesn't exist"))
    }
  }

  "trait Cookies#traversable" should {
    val cookie1 = Cookie(name = "cookie1", value = "value2")
    val cookie2 = Cookie(name = "cookie2", value = "value2")

    "be empty for no cookies" in {
      val c = Cookies(header = None)
      c must be empty
    }

    "contain elements for some cookies" in {
      val headerString = Cookies.encode(Seq(cookie1, cookie2))
      val c: Cookies = Cookies(header = Some(headerString))
      c must contain(allOf(cookie1, cookie2))
    }

    // technically the same as above
    "run a foreach for a cookie" in {
      val headerString = Cookies.encode(Seq(cookie1))
      val c: Cookies = Cookies(header = Some(headerString))

      var myCookie: Cookie = null
      c.foreach { cookie =>
        myCookie = cookie
      }
      myCookie must beEqualTo(cookie1)
    }
  }

}
