/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import org.specs2.mutable.Specification
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import scala.concurrent.{ Future, Promise }

object ServerResultUtilsSpec extends Specification with IterateeSpecification {

  case class CookieRequestHeader(cookie: Option[(String, String)]) extends RequestHeader {
    def id = 1
    def tags = Map()
    def uri = ""
    def path = ""
    def method = ""
    def version = ""
    def queryString = Map()
    def remoteAddress = ""
    def secure = false
    override def clientCertificateChain = None
    val headers = new Headers(cookie.map { case (name, value) => "Cookie" -> s"$name=$value" }.toSeq)
  }

  "ServerResultUtils.cleanFlashCookie" should {
    def flashCookieResult(cookie: Option[(String, String)], result: Result): Option[Seq[Cookie]] = {
      val rh = CookieRequestHeader(cookie)
      ServerResultUtils.cleanFlashCookie(rh, result).header.headers.get("Set-Cookie").map(Cookies.decodeSetCookieHeader)
    }

    "do nothing when flash not present" in {
      flashCookieResult(None, Ok) must beNone
    }
    "send flash if new" in {
      flashCookieResult(None, Ok.flashing("a" -> "b")) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== "a=b"
      }
    }
    "clear flash when received" in {
      flashCookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== ""
      }
    }
    "leave other cookies untouched when clearing" in {
      flashCookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok.withCookies(Cookie("cookie", "value"))) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 2
        cookies.find(_.name == "PLAY_FLASH") must beSome.like {
          case cookie => cookie.value must_== ""
        }
        cookies.find(_.name == "cookie") must beSome.like {
          case cookie => cookie.value must_== "value"
        }
      }
    }
    "clear old flash value when different value sent" in {
      flashCookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok.flashing("c" -> "d")) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== "c=d"
      }
    }
  }
}
