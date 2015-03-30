/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
    val headers = new Headers(cookie.map { case (name, value) => "Cookie" -> s"$name=$value" }.toSeq)
  }

  "ServerResultUtils.cleanFlashCookie" should {
    def flashCookieResult(cookie: Option[(String, String)], flash: Option[(String, String)]): Option[Seq[Cookie]] = {
      val rh = CookieRequestHeader(cookie)
      val result = flash.fold[Result](Ok) { case (name, value) => Ok.flashing(name -> value) }
      ServerResultUtils.cleanFlashCookie(rh, result).header.headers.get("Set-Cookie").map(Cookies.decode)
    }

    "do nothing when flash not present" in {
      flashCookieResult(None, None) must beNone
    }
    "send flash if new" in {
      flashCookieResult(None, Some("a" -> "b")) must beSome
    }
    "clear flash when received" in {
      flashCookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), None) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== ""
      }
    }
    "clear old flash value when different value sent" in {
      flashCookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Some("c" -> "d")) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== "c=d"
      }
    }
  }

  "ServerResultUtils.readAheadOne" should {

    "capture a 0-length stream" in {
      await(ServerResultUtils.readAheadOne(Enumerator() >>> Enumerator.eof)) must_== Left(None)
    }

    "capture a 1-length stream" in {
      await(ServerResultUtils.readAheadOne(Enumerator(1) >>> Enumerator.eof)) must_== Left(Option(1))
    }

    "not capture 2 0-length stream" in {
      await(ServerResultUtils.readAheadOne(Enumerator(1, 2) >>> Enumerator.eof)) must beRight { enum: Enumerator[Int] =>
        await(enum |>>> Iteratee.getChunks[Int]) must_== Seq(1, 2)
      }
    }

  }

}
