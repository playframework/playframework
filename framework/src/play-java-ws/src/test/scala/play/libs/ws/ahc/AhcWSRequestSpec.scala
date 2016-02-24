/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc

import org.specs2.mock.Mockito
import org.specs2.mutable._

class AhcWSRequestSpec extends Specification with Mockito {

  "AhcWSRequest" should {

    "should respond to getMethod" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.buildRequest().getMethod must be_==("GET")
    }

    "should set virtualHost appropriately" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setVirtualHost("foo.com")
      val actual = request.buildRequest().getVirtualHost()
      actual must beEqualTo("foo.com")
    }

    "should support setting a request timeout" in {
      requestWithTimeout(1000) must beEqualTo(1000)
    }

    "should support setting an infinite request timeout" in {
      requestWithTimeout(-1) must beEqualTo(-1)
    }

    "should not support setting a request timeout < -1" in {
      requestWithTimeout(-2) must throwA[IllegalArgumentException]
    }

    "should not support setting a request timeout > Integer.MAX_VALUE" in {
      requestWithTimeout(Int.MaxValue.toLong + 1) must throwA[IllegalArgumentException]
    }

    "Only send first content type header and add charset=utf-8 to the Content-Type header if it's manually adding but lacking charset" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setBody("HELLO WORLD")
      request.setHeader("Content-Type", "application/json")
      request.setHeader("Content-Type", "application/xml")
      val req = request.buildRequest()
      req.getHeaders.get("Content-Type") must be_==("application/json; charset=utf-8")
    }

    "Only send first content type header and keep the charset if it has been set manually with a charset" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setBody("HELLO WORLD")
      request.setHeader("Content-Type", "application/json; charset=US-ASCII")
      request.setHeader("Content-Type", "application/xml")
      val req = request.buildRequest()
      req.getHeaders.get("Content-Type") must be_==("application/json; charset=US-ASCII")
    }
  }

  def requestWithTimeout(timeout: Long) = {
    val client = mock[AhcWSClient]
    val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
    request.setRequestTimeout(timeout)
    request.buildRequest().getRequestTimeout()
  }
}
