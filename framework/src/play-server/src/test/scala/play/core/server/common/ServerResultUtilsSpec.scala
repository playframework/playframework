/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.http.Status._
import play.api.http.{ DefaultHttpErrorHandler, HttpEntity }
import play.api.libs.typedmap.TypedMap
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Success, Try }

class ServerResultUtilsSpec extends Specification {

  private def cookieRequestHeader(cookie: Option[(String, String)]): RequestHeader = {
    new RequestHeaderImpl(
      id = 1L,
      tags = Map.empty,
      uri = "",
      path = "",
      method = "",
      version = "",
      queryString = Map.empty,
      headers = new Headers(cookie.map { case (name, value) => "Cookie" -> s"$name=$value" }.toSeq),
      remoteAddress = "",
      secure = false,
      clientCertificateChain = None,
      attrMap = TypedMap.empty
    )
  }

  "ServerResultUtils.cleanFlashCookie" should {
    def flashCookieResult(cookie: Option[(String, String)], result: Result): Option[Seq[Cookie]] = {
      val rh = cookieRequestHeader(cookie)
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

  "ServerResultUtils#validateResult" should {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val header = new RequestHeaderImpl(
      id = 1L,
      tags = Map(),
      uri = "",
      path = "",
      method = "",
      version = "",
      queryString = Map(),
      remoteAddressFunc = () => "",
      secureFunc = () => false,
      clientCertificateChain = None,
      headers = new Headers(Seq()),
      attrMap = TypedMap.empty
    )

    def hasNoEntity(response: Future[Result], responseStatus: Int) = {
      Await.ready(response, 5.seconds)
      response.value must beSome[Try[Result]].like {
        case Success(res) =>
          res.header.status must_== responseStatus
          res.body must_== HttpEntity.NoEntity
          ok
        case _ =>
          ko("failed to obtain a response.")
      }
    }

    "cancel a message-body when a 100 response is returned" in {
      val response = ServerResultUtils.validateResult(header, Results.Continue, DefaultHttpErrorHandler)

      hasNoEntity(response, 100)
    }

    "cancel a message-body when a 101 response is returned" in {
      val response = ServerResultUtils.validateResult(header, Results.SwitchingProtocols, DefaultHttpErrorHandler)

      hasNoEntity(response, 101)
    }

    "cancel a message-body when a 204 response is returned" in {
      val response = ServerResultUtils.validateResult(header, Results.NoContent, DefaultHttpErrorHandler)

      hasNoEntity(response, 204)
    }

    "cancel a message-body when a 304 response is returned" in {
      val response = ServerResultUtils.validateResult(header, Results.NotModified, DefaultHttpErrorHandler)

      hasNoEntity(response, 304)
    }

    "cancel a message-body when a 100 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(CONTINUE), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = ServerResultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 100)
    }

    "cancel a message-body when a 101 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(SWITCHING_PROTOCOLS), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = ServerResultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 101)
    }

    "cancel a message-body when a 204 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(NO_CONTENT), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = ServerResultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 204)
    }

    "cancel a message-body when a 304 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(NOT_MODIFIED), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = ServerResultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 304)
    }
  }
}
