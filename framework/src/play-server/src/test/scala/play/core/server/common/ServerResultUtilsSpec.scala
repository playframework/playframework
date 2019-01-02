/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.http.Status._
import play.api.http._
import play.api.libs.crypto.CookieSignerProvider
import play.api.libs.typedmap.TypedMap
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.{ DefaultRequestFactory, RemoteConnection, RequestTarget }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Success, Try }

class ServerResultUtilsSpec extends Specification {

  val jwtCodec = new JWTCookieDataCodec {
    override def jwtConfiguration = JWTConfiguration()
    override def secretConfiguration = SecretConfiguration()
  }
  val resultUtils = {
    val httpConfig = HttpConfiguration()
    new ServerResultUtils(
      new DefaultSessionCookieBaker(httpConfig.session, httpConfig.secret, new CookieSignerProvider(httpConfig.secret).get),
      new DefaultFlashCookieBaker(httpConfig.flash, httpConfig.secret, new CookieSignerProvider(httpConfig.secret).get),
      new DefaultCookieHeaderEncoding(httpConfig.cookies)
    )
  }

  private def cookieRequestHeader(cookie: Option[(String, String)]): RequestHeader = {
    new DefaultRequestFactory(HttpConfiguration()).createRequestHeader(
      RemoteConnection("", false, None), "", RequestTarget("", "", Map.empty), "",
      new Headers(cookie.map { case (name, value) => "Cookie" -> s"$name=$value" }.toSeq),
      TypedMap.empty
    )
  }

  "resultUtils.prepareCookies" should {
    def cookieResult(cookie: Option[(String, String)], result: Result): Option[Seq[Cookie]] = {
      val encoding = new DefaultCookieHeaderEncoding()
      val rh = cookieRequestHeader(cookie)
      val newResult = resultUtils.prepareCookies(rh, result)
      newResult.header.headers.get("Set-Cookie").map(encoding.decodeSetCookieHeader)
    }

    "do nothing when flash not present" in {
      cookieResult(None, Ok) must beNone
    }
    "send flash if new" in {
      cookieResult(None, Ok.flashing("a" -> "b")) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        jwtCodec.decode(cookie.value) must_== Map("a" -> "b")
      }
    }
    "clear flash when received" in {
      cookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        cookie.value must_== ""
      }
    }
    "leave other cookies untouched when clearing" in {
      cookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok.withCookies(Cookie("cookie", "value"))) must beSome { cookies: Seq[Cookie] =>
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
      cookieResult(Some("PLAY_FLASH" -> "\"a=b\"; Path=/"), Ok.flashing("c" -> "d")) must beSome { cookies: Seq[Cookie] =>
        cookies.length must_== 1
        val cookie = cookies(0)
        cookie.name must_== "PLAY_FLASH"
        jwtCodec.decode(cookie.value) must_== Map("c" -> "d")
      }
    }
  }

  "resultUtils.validateResult" should {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val header = new RequestHeaderImpl(
      RemoteConnection("", false, None), "", RequestTarget("", "", Map.empty), "",
      Headers(), TypedMap.empty)

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
      val response = resultUtils.validateResult(header, Results.Continue, DefaultHttpErrorHandler)

      hasNoEntity(response, 100)
    }

    "cancel a message-body when a 101 response is returned" in {
      val response = resultUtils.validateResult(header, Results.SwitchingProtocols, DefaultHttpErrorHandler)

      hasNoEntity(response, 101)
    }

    "cancel a message-body when a 204 response is returned" in {
      val response = resultUtils.validateResult(header, Results.NoContent, DefaultHttpErrorHandler)

      hasNoEntity(response, 204)
    }

    "cancel a message-body when a 304 response is returned" in {
      val response = resultUtils.validateResult(header, Results.NotModified, DefaultHttpErrorHandler)

      hasNoEntity(response, 304)
    }

    "cancel a message-body when a 100 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(CONTINUE), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = resultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 100)
    }

    "cancel a message-body when a 101 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(SWITCHING_PROTOCOLS), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = resultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 101)
    }

    "cancel a message-body when a 204 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(NO_CONTENT), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = resultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 204)
    }

    "cancel a message-body when a 304 response with a non-empty body is returned" in {
      val result = Result(header = ResponseHeader(NOT_MODIFIED), body = HttpEntity.Strict(ByteString("foo"), None))
      val response = resultUtils.validateResult(header, result, DefaultHttpErrorHandler)

      hasNoEntity(response, 304)
    }
  }

  "resultUtils.validateHeaderNameChars" should {
    "accept Foo" in {
      resultUtils.validateHeaderNameChars("Foo") must not(throwAn[IllegalArgumentException])
    }
    "accept allowed chars" in {
      resultUtils.validateHeaderNameChars("!#$%&'*+-.^_`|~01239azAZ") must not(throwAn[IllegalArgumentException])
    }
    "not accept control characters" in {
      resultUtils.validateHeaderNameChars("\u0000") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderNameChars("\u0001") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderNameChars("\u001f") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderNameChars("\u00ff") must (throwAn[IllegalArgumentException])
    }
    "not accept delimiters" in {
      resultUtils.validateHeaderNameChars(":") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderNameChars(" ") must (throwAn[IllegalArgumentException])
    }
  }

  "resultUtils.validateHeaderValueChars" should {
    "accept bar" in {
      resultUtils.validateHeaderValueChars("bar") must not(throwAn[IllegalArgumentException])
    }
    "accept tokens" in {
      resultUtils.validateHeaderValueChars("!#$%&'*+-.^_`|~01239azAZ") must not(throwAn[IllegalArgumentException])
    }
    "accept separators" in {
      resultUtils.validateHeaderValueChars("\"(),/:;<=>?@[\\]{}") must not(throwAn[IllegalArgumentException])
    }
    "accept space and htab" in {
      resultUtils.validateHeaderValueChars(" \t") must not(throwAn[IllegalArgumentException])
    }
    "not accept control characters" in {
      resultUtils.validateHeaderValueChars("\u0000") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderValueChars("\u0001") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderValueChars("\u001f") must (throwAn[IllegalArgumentException])
      resultUtils.validateHeaderValueChars("\u007f") must (throwAn[IllegalArgumentException])
    }
  }
}
