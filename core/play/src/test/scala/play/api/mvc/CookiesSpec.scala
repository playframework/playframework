/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.time.{ Instant, ZoneId }

import org.specs2.mutable._
import play.api.http.{ JWTConfiguration, SecretConfiguration }
import play.api.mvc.Cookie.SameSite
import play.core.cookie.encoding.{ DefaultCookie, ServerCookieEncoder }
import play.core.test._

import scala.concurrent.duration._

class CookiesSpec extends Specification {

  sequential

  val Cookies = new DefaultCookieHeaderEncoding()

  "object Cookies#fromCookieHeader" should {

    "create new Cookies instance with cookies" in {
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

  "trait CookieHeaderEncoding#decodeSetCookieHeader" should {
    "parse empty string without exception " in {
      val decoded = Cookies.decodeSetCookieHeader("")
      decoded must be empty
    }
  }

  "ServerCookieEncoder" should {

    val encoder = ServerCookieEncoder.STRICT

    "properly encode ! character" in {
      val output = encoder.encode("TestCookie", "!")
      output must be_==("TestCookie=!")
    }

    // see #4460 for the gory details
    "properly encode all special characters" in {
      val output = encoder.encode("TestCookie", "!#$%&'()*+-./:<=>?@[]^_`{|}~")
      output must be_==("TestCookie=!#$%&'()*+-./:<=>?@[]^_`{|}~")
    }

    "properly encode field name which starts with $" in {
      val output = encoder.encode("$Test", "Test")
      output must be_==("$Test=Test")
    }

    "properly encode discarded cookies" in {
      val dc = new DefaultCookie("foo", "bar")
      dc.setMaxAge(0)
      val encoded = encoder.encode(dc)
      encoded must_== "foo=bar; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
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

    "return none if no cookie" in {
      c.get("no-cookie") must beNone
    }
  }

  "trait Cookies#apply" should {
    val originalCookie = Cookie(name = "cookie", value = "value")
    def headerString = Cookies.encodeCookieHeader(Seq(originalCookie))
    def c: Cookies = Cookies.fromCookieHeader(Some(headerString))

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
      val c = Cookies.fromCookieHeader(header = None)
      c must be empty
    }

    "contain elements for some cookies" in {
      val headerString = Cookies.encodeCookieHeader(Seq(cookie1, cookie2))
      val c: Cookies = Cookies.fromCookieHeader(Some(headerString))
      c must contain(allOf(cookie1, cookie2))
    }

    // technically the same as above
    "run a foreach for a cookie" in {
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

    "handle __Host cookies properly" in {
      val decoded = Cookies.decodeSetCookieHeader("__Host-ID=123; Secure; Path=/")
      decoded must contain(Cookie("__Host-ID", "123", secure = true, httpOnly = false, path = "/"))
    }
    "handle __Secure cookies properly" in {
      val decoded = Cookies.decodeSetCookieHeader("__Secure-ID=123; Secure")
      decoded must contain(Cookie("__Secure-ID", "123", secure = true, httpOnly = false))
    }
    "handle SameSite cookies properly" in {
      val decoded = Cookies.decodeSetCookieHeader("__Secure-ID=123; Secure; SameSite=strict")
      decoded must contain(Cookie("__Secure-ID", "123", secure = true, httpOnly = false, sameSite = Some(SameSite.Strict)))
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

  class TestJWTCookieDataCodec extends JWTCookieDataCodec {
    val secretConfiguration = SecretConfiguration()
    val jwtConfiguration = JWTConfiguration()
    override protected def uniqueId(): Option[String] = None
    override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("UTC"))
  }

  "trait JWTCookieData" should {
    val codec = new TestJWTCookieDataCodec()

    "encode map to string" in {
      val jwtValue = codec.encode(Map("hello" -> "world"))
      jwtValue must beEqualTo("eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImhlbGxvIjoid29ybGQifSwibmJmIjowLCJpYXQiOjB9.mQUJopezrr3EC9gn_sB4XMb0ahvVq5F3tTB1shH0UOk")
    }

    "decode string to map" in {
      val jwtValue = "eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImhlbGxvIjoid29ybGQifSwibmJmIjowLCJpYXQiOjB9.mQUJopezrr3EC9gn_sB4XMb0ahvVq5F3tTB1shH0UOk"
      codec.decode(jwtValue) must contain("hello" -> "world")
    }

    "decode empty string to map" in {
      val jwtValue = ""
      codec.decode(jwtValue) must beEmpty
    }

    "encode and decode in a round trip" in {
      val jwtValue = codec.encode(Map("hello" -> "world"))
      codec.decode(jwtValue) must contain("hello" -> "world")
    }

    "return empty map given a bad string" in {
      val jwtValue = ".eyJuYmYiOjAsImlhdCI6MCwiZGF0YSI6eyJoZWxsbyI6IndvcmxkIn19.SoN8DSDXnFSK0oZXs6hsP4y_8MQqiWQAPJYiTNfAErM"
      codec.decode(jwtValue) must beEmpty
    }

    "return empty map given a JWT with a bad signatureAlgorithm" in {
      val goodCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(signatureAlgorithm = "HS256")
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("UTC"))
      }

      // alg: "none"
      val badJwt = "eyJhbGciOiJub25lIn0.eyJuYmYiOjAsImlhdCI6MCwiZGF0YSI6eyJoZWxsbyI6IndvcmxkIn19.Xv7-BTFyhGvi_NavNvQpvcPf1clHijcei-1EFlSLfLQ"
      goodCodec.decode(badJwt) must beEmpty
    }

    "return empty map given an expired JWT outside of clock skew" in {
      val oldCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(expiresAfter = Some(5.seconds))
      }

      val newCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(clockSkew = 60.seconds)
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(80000), ZoneId.of("UTC"))
      }

      val oldJwt = oldCodec.encode(Map("hello" -> "world"))
      newCodec.decode(oldJwt) must beEmpty
    }

    "return value given an expired JWT inside of clock skew" in {
      val oldCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(expiresAfter = Some(10.seconds))
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("UTC"))
      }

      val newCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(clockSkew = 60.seconds)

        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(60000), ZoneId.of("UTC"))
      }

      val oldJwt = oldCodec.encode(Map("hello" -> "world"))
      newCodec.decode(oldJwt) must contain("hello" -> "world")
    }

    "return empty map given a not before JWT outside of clock skew" in {
      val oldCodec = new TestJWTCookieDataCodec

      val newCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(clockSkew = 60.seconds)
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(80000), ZoneId.of("UTC"))
      }

      val newJwt = newCodec.encode(Map("hello" -> "world"))
      oldCodec.decode(newJwt) must beEmpty
    }

    "return value given a not before JWT inside of clock skew" in {
      val oldCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(clockSkew = 60.seconds)
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("UTC"))
      }

      val newCodec = new TestJWTCookieDataCodec {
        override val jwtConfiguration = JWTConfiguration(clockSkew = 60.seconds)
        override val clock = java.time.Clock.fixed(Instant.ofEpochMilli(60000), ZoneId.of("UTC"))
      }

      val newJwt = newCodec.encode(Map("hello" -> "world"))
      oldCodec.decode(newJwt) must contain("hello" -> "world")
    }
  }

  "DefaultSessionCookieBaker" should {
    val sessionCookieBaker = new DefaultSessionCookieBaker() {
      override val jwtCodec: JWTCookieDataCodec = new TestJWTCookieDataCodec()
    }

    "decode a signed cookie encoding" in {
      val signedEncoding = "116d8da7c5283e81341db8a0c0fb5f188f9b0277-hello=world"
      sessionCookieBaker.decode(signedEncoding) must contain("hello" -> "world")
    }

    "decode a JWT cookie encoding" in {
      val signedEncoding = "eyJhbGciOiJIUzI1NiJ9.eyJuYmYiOjAsImlhdCI6MCwiZGF0YSI6eyJoZWxsbyI6IndvcmxkIn19.SoN8DSDXnFSK0oZXs6hsP4y_8MQqiWQAPJYiTNfAErM"
      sessionCookieBaker.decode(signedEncoding) must contain("hello" -> "world")
    }

    "decode an empty cookie" in {
      sessionCookieBaker.decode("") must beEmpty
    }

    "decode an empty legacy session" in {
      val signedEncoding = "116d8da7c5283e81341db8a0c0fb5f188f9b0277"
      sessionCookieBaker.decode(signedEncoding) must beEmpty
    }

    "encode to JWT" in {
      val jwtEncoding = "eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImhlbGxvIjoid29ybGQifSwibmJmIjowLCJpYXQiOjB9.mQUJopezrr3EC9gn_sB4XMb0ahvVq5F3tTB1shH0UOk"
      sessionCookieBaker.encode(Map("hello" -> "world")) must beEqualTo(jwtEncoding)
    }
  }

  "LegacySessionCookieBaker" should {
    val legacyCookieBaker = new LegacySessionCookieBaker()

    "encode to a signed string" in {
      val encoding = legacyCookieBaker.encode(Map("hello" -> "world"))

      encoding must beEqualTo("116d8da7c5283e81341db8a0c0fb5f188f9b0277-hello=world")
    }
  }

}
