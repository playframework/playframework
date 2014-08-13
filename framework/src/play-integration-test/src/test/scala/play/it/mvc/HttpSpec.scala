package play.it.mvc

import play.api.libs.Files.TemporaryFile

import play.api.http.HeaderNames
import play.api.test.{ FakeHeaders, FakeRequest }

import play.api.mvc.{ 
  AnyContentAsFormUrlEncoded, 
  AnyContentAsMultipartFormData, 
  MultipartFormData,
  Call 
}

object HttpSpec extends org.specs2.mutable.Specification {
  "HTTP" title

  "Absolute URL" should {
    val req = FakeRequest().withHeaders(HeaderNames.HOST -> "playframework.com")

    "have HTTP scheme" in {
      (Call("GET", "/playframework").absoluteURL()(req).
        aka("absolute URL 1") must_== "http://playframework.com/playframework").
        and(Call("GET", "/playframework").absoluteURL(false)(req).
          aka("absolute URL 2") must_== (
            "http://playframework.com/playframework"))
    }

    "have HTTPS scheme" in {
      (Call("GET", "/playframework").absoluteURL()(req.copy(secure = true)).
        aka("absolute URL 1") must_== (
          "https://playframework.com/playframework")) and (
        Call("GET", "/playframework").absoluteURL(true)(req).
          aka("absolute URL 2") must_== (
            "https://playframework.com/playframework"))
    }
  }

  "Web socket URL" should {
    val req = FakeRequest().withHeaders(
      HeaderNames.HOST -> "playframework.com")

    "have WS scheme" in {
      (Call("GET", "/playframework").webSocketURL()(req).
        aka("absolute URL 1") must_== "ws://playframework.com/playframework").
        and(Call("GET", "/playframework").webSocketURL(false)(req).
          aka("absolute URL 2") must_== (
            "ws://playframework.com/playframework"))
    }

    "have WSS scheme" in {
      (Call("GET", "/playframework").webSocketURL()(req.copy(secure = true)).
        aka("absolute URL 1") must_== (
          "wss://playframework.com/playframework")) and (
        Call("GET", "/playframework").webSocketURL(true)(req).
          aka("absolute URL 2") must_== (
            "wss://playframework.com/playframework"))
    }
  }

  "RequestHeader" should {
    "parse quoted and unquoted charset" in {
      FakeRequest().withHeaders(
        HeaderNames.CONTENT_TYPE -> """text/xml; charset="utf-8"""").
        charset aka "request charset" must beSome("utf-8")
    }

    "parse quoted and unquoted charset" in {
      FakeRequest().withHeaders(
        HeaderNames.CONTENT_TYPE -> "text/xml; charset=utf-8").
        charset aka "request charset" must beSome("utf-8")
    }
  }

  "Parameters" >> {
    "without form data" should {
      "be only from query string" in {
        FakeRequest("GET", "http://localhost/?a=b&a=c&d=e").
          parameters aka "parameters" must_== Map(
            "a" -> Seq("b", "c"), "d" -> Seq("e"))
      }
    }

    "with url encoded body" should {
      "be only from query string with empty body" in {
        val urlencoded = AnyContentAsFormUrlEncoded(Map.empty)

        FakeRequest("GET", "http://localhost/?a=b&a=c&d=e", FakeHeaders.empty,
          body = urlencoded).parameters aka "parameters" must_== Map(
          "a" -> Seq("b", "c"), "d" -> Seq("e"))
      }

      "be from query string and from body" in {
        val urlencoded = AnyContentAsFormUrlEncoded(
          Map("d" -> Seq("f", "g"), "h" -> Nil))

        FakeRequest("GET", "http://localhost/?a=b&a=c&d=e", FakeHeaders.empty,
          body = urlencoded).parameters aka "parameters" must_== Map(
          "a" -> Seq("b", "c"), "d" -> Seq("e", "f", "g"), "h" -> Nil)
      }

      "be only from body" in {
        val urlencoded = AnyContentAsFormUrlEncoded(
          Map("d" -> Seq("f", "g"), "h" -> Nil))

        FakeRequest("GET", "http://localhost/", FakeHeaders.empty,
          body = urlencoded).parameters aka "parameters" must_== Map(
          "d" -> Seq("f", "g"), "h" -> Nil)
      }
    }

    "with multipart body" should {
      "be only from query string with empty body" in {
        val bodyParams = Map.empty[String,Seq[String]]
        val multipart = AnyContentAsMultipartFormData(
          MultipartFormData[TemporaryFile](bodyParams, Nil, Nil, Nil))

        multipart.asMultipartFormData.map(_.asFormUrlEncoded).
          aka("multipart parameters") must_== Some(bodyParams) and (
            FakeRequest("GET", "http://localhost/?a=b&a=c&d=e", 
              FakeHeaders.empty, body = multipart).parameters.
              aka("parameters") must_== Map(
                "a" -> Seq("b", "c"), "d" -> Seq("e")))

      }

      "be from query string and from body" in {
        val bodyParams = Map("a" -> Seq("foo"), "f" -> Seq("g", "h"))
        val multipart = AnyContentAsMultipartFormData(
          MultipartFormData[TemporaryFile](bodyParams, Nil, Nil, Nil))

        multipart.asMultipartFormData.map(_.asFormUrlEncoded).
          aka("multipart parameters") must_== Some(bodyParams) and (
            FakeRequest("GET", "http://localhost/?a=b&a=c&d=e", 
              FakeHeaders.empty, body = multipart).parameters.
              aka("parameters") must_== Map("a" -> Seq("b", "c", "foo"), 
                "d" -> Seq("e"), "f" -> Seq("g", "h")))

      }

      "be only from body" in {
        val bodyParams = Map("a" -> Seq("foo"), "f" -> Seq("g", "h"))
        val multipart = AnyContentAsMultipartFormData(
          MultipartFormData[TemporaryFile](bodyParams, Nil, Nil, Nil))

        multipart.asMultipartFormData.map(_.asFormUrlEncoded).
          aka("multipart parameters") must_== Some(bodyParams) and (
            FakeRequest("GET", "http://localhost/", 
              FakeHeaders.empty, body = multipart).parameters.
              aka("parameters") must_== Map(
                "a" -> Seq("foo"), "f" -> Seq("g", "h")))

      }
    }
  }
}
