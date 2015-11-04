/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc

import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.libs.ws.{ WSRequestExecutor, WSRequestFilter }
import play.libs.oauth.OAuth

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

    "Have form body on POST of content type text/plain" in {
      val client = mock[AhcWSClient]
      val formEncoding = java.net.URLEncoder.encode("param1=value1", "UTF-8")

      val ahcRequest = new AhcWSRequest(client, "http://playframework.com/", null)
        .setHeader("Content-Type", "text/plain")
        .setBody("HELLO WORLD")
        .asInstanceOf[AhcWSRequest]
      val req = ahcRequest.buildRequest()
      req.getStringData must be_==("HELLO WORLD")
    }

    "Have form body on POST of content type application/x-www-form-urlencoded explicitly set" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val req = new AhcWSRequest(client, "http://playframework.com/", null)
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("HELLO WORLD") // and body is set to string (see #5221)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getStringData must be_==("HELLO WORLD") // should result in byte data.
    }

    "Have form params on POST of content type application/x-www-form-urlencoded when signed" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val consumerKey = new OAuth.ConsumerKey("key", "secret")
      val token = new OAuth.RequestToken("token", "secret")
      val calc = new OAuth.OAuthCalculator(consumerKey, token)
      val req = new AhcWSRequest(client, "http://playframework.com/", null)
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("param1=value1")
        .sign(calc)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      // Note we use getFormParams instead of getByteData here.
      req.getFormParams.asScala must containTheSameElementsAs(List(new org.asynchttpclient.Param("param1", "value1")))
    }

    "Remove a user defined content length header if we are parsing body explicitly when signed" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val consumerKey = new OAuth.ConsumerKey("key", "secret")
      val token = new OAuth.RequestToken("token", "secret")
      val calc = new OAuth.OAuthCalculator(consumerKey, token)
      val req = new AhcWSRequest(client, "http://playframework.com/", null)
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("param1=value1")
        .setHeader("Content-Length", "9001") // add a meaningless content length here...
        .sign(calc)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()

      val headers = req.getHeaders
      req.getFormParams.asScala must containTheSameElementsAs(List(new org.asynchttpclient.Param("param1", "value1")))
      headers.get("Content-Length") must beNull // no content length!
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

    "Only send first content type header" in {
      import scala.collection.JavaConverters._
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setBody("HELLO WORLD")
      request.setHeader("Content-Type", "application/json")
      request.setHeader("Content-Type", "application/xml")
      val req = request.buildRequest()
      req.getHeaders.get("Content-Type") must be_==("application/json")
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
