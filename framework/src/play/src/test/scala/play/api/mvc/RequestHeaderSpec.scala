/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.security.cert.X509Certificate

import org.specs2.mutable.Specification
import play.api.http.HeaderNames._
import play.api.http.HttpConfiguration
import play.api.i18n.Lang
import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc.request.{ DefaultRequestFactory, RemoteConnection, RequestTarget }

class RequestHeaderSpec extends Specification {

  "request header" should {

    "convert to java" in {
      "keep all the headers" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.asJava.getHeaders.contains(HOST) must beTrue
      }
      "keep the headers accessible case insensitively" in {
        val rh = dummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.asJava.getHeaders.contains("host") must beTrue
      }
    }

    "have typed attributes" in {
      "can set and get a single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().withAttrs(TypedMap(x -> 3)).attrs(x) must_== 3
      }
      "can set two attributes and get one back" in {
        val x = TypedKey[Int]("x")
        val y = TypedKey[String]("y")
        dummyRequestHeader().withAttrs(TypedMap(x -> 3, y -> "hello")).attrs(y) must_== "hello"
      }
      "getting a set attribute should be Some" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().withAttrs(TypedMap(x -> 5)).attrs.get(x) must beSome(5)
      }
      "getting a nonexistent attribute should be None" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().attrs.get(x) must beNone
      }
      "can add single attribute" in {
        val x = TypedKey[Int]("x")
        dummyRequestHeader().addAttr(x, 3).attrs(x) must_== 3
      }
      "keep current attributes when adding a new one" in {
        val x = TypedKey[Int]
        val y = TypedKey[String]
        dummyRequestHeader().withAttrs(TypedMap(y -> "hello")).addAttr(x, 3).attrs(y) must_== "hello"
      }
      "overrides current attribute value" in {
        val x = TypedKey[Int]
        val y = TypedKey[String]
        val requestHeader = dummyRequestHeader().withAttrs(TypedMap(y -> "hello"))
          .addAttr(x, 3)
          .addAttr(y, "white")

        requestHeader.attrs(y) must_== "white"
        requestHeader.attrs(x) must_== 3
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

    "deprecated copy method" in {

      def checkRequestValues(
        origReq: RequestHeader,
        changeReq: RequestHeader => RequestHeader)(
        id: Long = origReq.id,
        uri: String = origReq.uri,
        path: String = origReq.path,
        method: String = origReq.method,
        version: String = origReq.version,
        queryString: Map[String, Seq[String]] = origReq.queryString,
        headers: Headers = origReq.headers,
        remoteAddress: String = origReq.remoteAddress,
        secure: Boolean = origReq.secure,
        clientCertificateChain: Option[Seq[X509Certificate]] = origReq.clientCertificateChain) = {
        val newReq: RequestHeader = changeReq(origReq)
        newReq.id must_== id
        newReq.uri must_== uri
        newReq.path must_== path
        newReq.method must_== method
        newReq.version must_== version
        newReq.queryString must_== queryString
        newReq.headers must_== headers
        newReq.remoteAddress must_== remoteAddress
        newReq.secure must_== secure
        newReq.clientCertificateChain must_== clientCertificateChain
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
    new DefaultRequestFactory(HttpConfiguration()).createRequestHeader(
      connection = RemoteConnection("", false, None),
      method = requestMethod,
      target = RequestTarget(requestUri, "", Map.empty),
      version = "",
      headers = headers,
      attrs = TypedMap.empty
    )
  }
}
