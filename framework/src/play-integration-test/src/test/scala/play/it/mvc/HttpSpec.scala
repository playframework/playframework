/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import play.api.http.HeaderNames
import play.api.test.FakeRequest

object HttpSpec extends org.specs2.mutable.Specification {

  title("HTTP")

  "Absolute URL" should {
    val req = FakeRequest().withHeaders(HeaderNames.HOST -> "playframework.com")

    "have HTTP scheme" in {
      (Call("GET", "/playframework").absoluteURL()(req).
        aka("absolute URL 1") must_== "http://playframework.com/playframework").
        and(Call("GET", "/playframework").absoluteURL(secure = false)(req).
          aka("absolute URL 2") must_== (
            "http://playframework.com/playframework"))
    }

    "have HTTPS scheme" in {
      (Call("GET", "/playframework").absoluteURL()(req.copy(secure = true)).
        aka("absolute URL 1") must_== (
          "https://playframework.com/playframework")) and (
            Call("GET", "/playframework").absoluteURL(secure = true)(req).
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
        and(Call("GET", "/playframework").webSocketURL(secure = false)(req).
          aka("absolute URL 2") must_== (
            "ws://playframework.com/playframework"))
    }

    "have WSS scheme" in {
      (Call("GET", "/playframework").webSocketURL()(req.copy(secure = true)).
        aka("absolute URL 1") must_== (
          "wss://playframework.com/playframework")) and (
            Call("GET", "/playframework").webSocketURL(secure = true)(req).
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
}
