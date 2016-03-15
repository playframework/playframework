/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.net.URI

import org.specs2.mutable.Specification
import play.api.http.HeaderNames._
import play.api.i18n.Lang

class RequestHeaderSpec extends Specification {

  "request header" should {

    "handle host" in {
      "relative uri with host header" in {
        val rh = DummyRequestHeader("GET", "/", Headers(HOST -> "playframework.com"))
        rh.host must_== "playframework.com"
      }
      "absolute uri" in {
        val rh = DummyRequestHeader("GET", "https://example.com/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com"
      }
      "absolute uri with port" in {
        val rh = DummyRequestHeader("GET", "https://example.com:8080/test", Headers(HOST -> "playframework.com"))
        rh.host must_== "example.com:8080"
      }
    }

    "parse accept languages" in {

      "return an empty sequence when no accept languages specified" in {
        DummyRequestHeader().acceptLanguages must beEmpty
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

  def accept(value: String) = DummyRequestHeader(
    headers = Headers("Accept-Language" -> value)
  ).acceptLanguages

  case class DummyRequestHeader(
      requestMethod: String = "GET",
      requestUri: String = "/",
      headers: Headers = Headers()) extends RequestHeader {
    private[this] val parsedUri = new URI(requestUri)
    def id = 1
    def tags = Map()
    def uri = requestUri
    def path = parsedUri.getPath
    def method = requestMethod
    def version = ""
    def queryString = Map()
    def remoteAddress = ""
    def secure = false
    override def clientCertificateChain = None
  }
}
