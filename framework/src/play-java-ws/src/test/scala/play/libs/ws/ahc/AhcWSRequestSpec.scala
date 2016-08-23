/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc

import org.asynchttpclient.{ Request, RequestBuilderBase, SignatureCalculator }
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.libs.ws.{ WSAuthScheme, WSSignatureCalculator }
import play.libs.oauth.OAuth

class AhcWSRequestSpec extends Specification with Mockito {

  "AhcWSRequest" should {

    "respond to getMethod" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.buildRequest().getMethod must be_==("GET")
    }

    "set virtualHost appropriately" in {
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

    "Use a custom signature calculator" in {
      val client = mock[AhcWSClient]
      var called = false
      val calc = new SignatureCalculator with WSSignatureCalculator {
        override def calculateAndAddSignature(request: Request, requestBuilder: RequestBuilderBase[_]): Unit = {
          called = true
        }
      }
      val req = new AhcWSRequest(client, "http://playframework.com/", null)
        .sign(calc)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      called must beTrue
    }

    "support setting a request timeout" in {
      requestWithTimeout(1000) must beEqualTo(1000)
    }

    "support setting an infinite request timeout" in {
      requestWithTimeout(-1) must beEqualTo(-1)
    }

    "not support setting a request timeout < -1" in {
      requestWithTimeout(-2) must throwA[IllegalArgumentException]
    }

    "not support setting a request timeout > Integer.MAX_VALUE" in {
      requestWithTimeout(Int.MaxValue.toLong + 1) must throwA[IllegalArgumentException]
    }

    "set a query string appropriately" in {
      val queryParams = requestWithQueryString("q=playframework&src=typd")
      queryParams.size must beEqualTo(2)
      queryParams.exists(p => (p.getName == "q") && (p.getValue == "playframework")) must beTrue
      queryParams.exists(p => (p.getName == "src") && (p.getValue == "typd")) must beTrue
    }

    "support several query string values for a parameter" in {
      val queryParams = requestWithQueryString("q=scala&q=playframework&q=fp")
      queryParams.size must beEqualTo(3)
      queryParams.exists(p => (p.getName == "q") && (p.getValue == "scala")) must beTrue
      queryParams.exists(p => (p.getName == "q") && (p.getValue == "playframework")) must beTrue
      queryParams.exists(p => (p.getName == "q") && (p.getValue == "fp")) must beTrue
      queryParams.count(p => p.getName == "q") must beEqualTo(3)
    }

    "support a query string parameter without value" in {
      val queryParams = requestWithQueryString("q=playframework&src=")
      queryParams.size must beEqualTo(2)
      queryParams.exists(p => (p.getName == "q") && (p.getValue == "playframework")) must beTrue
      queryParams.exists(p => (p.getName.equals("src")) && (p.getValue == null)) must beTrue
    }

    "not support a query string with more than 2 = per part" in {
      requestWithQueryString("q=scala=playframework&src=typd") must throwA[RuntimeException]
    }

    "not support a query string if it starts with = and is empty" in {
      requestWithQueryString("=&src=typd") must throwA[RuntimeException]
    }

    "only send first content type header and add charset=utf-8 to the Content-Type header if it's manually adding but lacking charset" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setBody("HELLO WORLD")
      request.setHeader("Content-Type", "application/json")
      request.setHeader("Content-Type", "application/xml")
      val req = request.buildRequest()
      req.getHeaders.get("Content-Type") must be_==("application/json")
    }

    "only send first content type header and keep the charset if it has been set manually with a charset" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setBody("HELLO WORLD")
      request.setHeader("Content-Type", "application/json; charset=US-ASCII")
      request.setHeader("Content-Type", "application/xml")
      val req = request.buildRequest()
      req.getHeaders.get("Content-Type") must be_==("application/json; charset=US-ASCII")
    }

    "Set Realm.UsePreemptiveAuth to false when WSAuthScheme.DIGEST being used" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setAuth("usr", "pwd", WSAuthScheme.DIGEST)
      val req = request.buildRequest()
      req.getRealm.isUsePreemptiveAuth must beFalse
    }

    "Set Realm.UsePreemptiveAuth to true when WSAuthScheme.DIGEST not being used" in {
      val client = mock[AhcWSClient]
      val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
      request.setAuth("usr", "pwd", WSAuthScheme.BASIC)
      val req = request.buildRequest()
      req.getRealm.isUsePreemptiveAuth must beTrue
    }
  }

  def requestWithTimeout(timeout: Long) = {
    val client = mock[AhcWSClient]
    val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
    request.setRequestTimeout(timeout)
    request.buildRequest().getRequestTimeout()
  }

  def requestWithQueryString(query: String) = {
    import scala.collection.JavaConverters._
    val client = mock[AhcWSClient]
    val request = new AhcWSRequest(client, "http://example.com", /*materializer*/ null)
    request.setQueryString(query)
    val queryParams = request.buildRequest().getQueryParams
    queryParams.asScala.toSeq
  }
}
