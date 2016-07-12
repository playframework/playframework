/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.net.URI

import org.specs2.mutable.Specification
import play.api.http.HeaderNames._
import play.api.i18n.Lang
import play.api.libs.typedmap.{ TypedKey, TypedMap }

class RequestHeaderSpec extends Specification {

  "request header" should {

    "have typed properties" in {
      "can set and get a single property" in {
        val x = TypedKey[Int]("x")
        (dummyRequestHeader() + (x -> 3))(x) must_== 3
      }
      "can set two properties and get one back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        (dummyRequestHeader() + (x -> 3, y -> "hello"))(y) must_== "hello"
      }
      "getting a set property should be Some" in {
        val x = TypedKey[Int]("x")
        (dummyRequestHeader() + (x -> 5)).get(x) must beSome(5)
      }
      "getting a nonexistent property should be None" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().get(x) must beNone
      }
    }

    "handle host" in {
      "relative uri with host header" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.host must_== "playframework.com"
      }
      "absolute uri" in {
        val rh = dummyRequestHeader("GET", "https://example.com/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com"
      }
      "absolute uri with port" in {
        val rh = dummyRequestHeader("GET", "https://example.com:8080/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com:8080"
      }
      "absolute uri with port and invalid characters" in {
        val rh = dummyRequestHeader("GET", "https://example.com:8080/classified-search/classifieds?version=GTI|V8", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com:8080"
      }
      "relative uri with invalid characters" in {
        val rh = dummyRequestHeader("GET", "/classified-search/classifieds?version=GTI|V8", Headers(HOST -> "playframework.com"))
        rh.host must_== "playframework.com"
      }
    }

    "parse accept languages" in {

      "return an empty sequence when no accept languages specified" in {
        dummyRequestHeader().acceptLanguages must beEmpty
      }

      "parse a single accept language" in {
        accept("en") must contain(exactly(Lang("en")))
      }

      "parse a single accept language and country" in {
        accept("en-US") must contain(exactly(Lang("en-US")))
      }

      "parse multiple accept languages" in {
        accept("en-US, es") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
      }

      "sort accept languages by quality" in {
        accept("en-US;q=0.8, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es;q=0.8") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }

      "default accept language quality to 1" in {
        accept("en-US, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }

    }
  }

  private def accept(value: String) = dummyRequestHeader(
    headers = Headers("Accept-Language" -> value)
  ).acceptLanguages

  private def dummyRequestHeader(
    requestMethod: String = "GET",
    requestUri: String = "/",
    headers: Headers = Headers()): RequestHeader = {
    new RequestHeaderImpl(
      id = 1L,
      tags = Map.empty,
      uri = requestUri,
      path = "",
      method = requestMethod,
      version = "",
      queryString = Map.empty,
      headers = headers,
      remoteAddress = "",
      secure = false,
      clientCertificateChain = None,
      properties = TypedMap.empty
    )
  }
}
