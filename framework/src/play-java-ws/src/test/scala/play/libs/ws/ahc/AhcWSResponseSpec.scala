/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc

import java.nio.charset.StandardCharsets

import io.netty.handler.codec.http.DefaultHttpHeaders

import scala.collection.JavaConverters._

import org.specs2.mock.Mockito
import org.specs2.mutable._

import org.asynchttpclient.{ Response }

object AhcWSResponseSpec extends Specification with Mockito {

  private val emptyMap = new java.util.HashMap[String, java.util.Collection[String]]

  "getUnderlying" should {

    "return the underlying response" in {
      val srcResponse = mock[Response]
      val response = new AhcWSResponse(srcResponse)
      response.getUnderlying must_== (srcResponse)
    }

  }

  "getAllHeaders" should {
    "get headers map which retrieves headers case insensitively" in {
      val srcResponse = mock[Response]
      val srcHeaders = new DefaultHttpHeaders()
        .add("Foo", "a")
        .add("foo", "b")
        .add("FOO", "b")
        .add("Bar", "baz")
      srcResponse.getHeaders returns srcHeaders
      val response = new AhcWSResponse(srcResponse)
      val headers = response.getAllHeaders
      headers.get("foo").asScala must_== Seq("a", "b", "b")
      headers.get("BAR").asScala must_== Seq("baz")
    }

  }

  "getBody" should {

    "get the body as UTF-8 by default when no content type" in {
      val ahcResponse = mock[Response]
      val response = new AhcWSResponse(ahcResponse)
      ahcResponse.getContentType returns null
      ahcResponse.getResponseBody(any) returns "body"

      val body = response.getBody
      there was one(ahcResponse).getResponseBody(StandardCharsets.UTF_8)
      body must be_==("body")
    }

    "get the body as ISO_8859_1 by default when content type text/plain without charset" in {
      val ahcResponse = mock[Response]
      val response = new AhcWSResponse(ahcResponse)
      ahcResponse.getContentType returns "text/plain"
      ahcResponse.getResponseBody(any) returns "body"

      val body = response.getBody
      there was one(ahcResponse).getResponseBody(StandardCharsets.ISO_8859_1)
      body must be_==("body")
    }

    "get the body as given charset when content type has explicit charset" in {
      val ahcResponse = mock[Response]
      val response = new AhcWSResponse(ahcResponse)
      ahcResponse.getContentType returns "text/plain; charset=UTF-16"
      ahcResponse.getResponseBody(any) returns "body"

      val body = response.getBody
      there was one(ahcResponse).getResponseBody(StandardCharsets.UTF_16)
      body must be_==("body")
    }
  }

  /*
  getStatus
  getStatusText
  getHeader
  getCookies
  getCookie
  getBody
  asXml
  asJson
  getBodyAsStream
  asByteArray
  getUri
  */

}
