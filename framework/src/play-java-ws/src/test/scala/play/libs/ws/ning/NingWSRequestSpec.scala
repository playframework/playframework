package play.libs.ws.ning

import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.test.WithApplication
import play.libs.oauth.OAuth

class NingWSRequestSpec extends Specification with Mockito {

  "NingWSRequest" should {

    "should respond to getMethod" in {
      val client = mock[NingWSClient]
      val request = new NingWSRequest(client, "http://example.com")
      request.buildRequest().getMethod must be_==("GET")
    }

    "should set virtualHost appropriately" in {
      val client = mock[NingWSClient]
      val request = new NingWSRequest(client, "http://example.com")
      request.setVirtualHost("foo.com")
      val actual = request.buildRequest().getVirtualHost()
      actual must beEqualTo("foo.com")
    }

    "Have form body on POST of content type text/plain" in new WithApplication {
      val client = mock[NingWSClient]
      val formEncoding = java.net.URLEncoder.encode("param1=value1", "UTF-8")

      val ningRequest = new NingWSRequest(client, "http://playframework.com/")
        .setHeader("Content-Type", "text/plain")
        .setBody("HELLO WORLD")
        .asInstanceOf[NingWSRequest]
      val req = ningRequest.buildRequest()
      req.getStringData must be_==("HELLO WORLD")
    }

    "Have form body on POST of content type application/x-www-form-urlencoded explicitly set" in new WithApplication {
      import scala.collection.JavaConverters._
      val client = mock[NingWSClient]
      val req = new NingWSRequest(client, "http://playframework.com/")
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("HELLO WORLD") // and body is set to string (see #5221)
        .asInstanceOf[NingWSRequest]
        .buildRequest()
      req.getStringData must be_==("HELLO WORLD") // should result in byte data.
    }

    "Have form params on POST of content type application/x-www-form-urlencoded when signed" in new WithApplication {
      import scala.collection.JavaConverters._
      val client = mock[NingWSClient]
      val consumerKey = new OAuth.ConsumerKey("key", "secret")
      val token = new OAuth.RequestToken("token", "secret")
      val calc = new OAuth.OAuthCalculator(consumerKey, token)
      val req = new NingWSRequest(client, "http://playframework.com/")
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("param1=value1")
        .sign(calc)
        .asInstanceOf[NingWSRequest]
        .buildRequest()
      // Note we use getFormParams instead of getByteData here.
      req.getFormParams.asScala must containTheSameElementsAs(List(new com.ning.http.client.Param("param1", "value1")))
    }

    "Remove a user defined content length header if we are parsing body explicitly when signed" in new WithApplication {
      import scala.collection.JavaConverters._
      val client = mock[NingWSClient]
      val consumerKey = new OAuth.ConsumerKey("key", "secret")
      val token = new OAuth.RequestToken("token", "secret")
      val calc = new OAuth.OAuthCalculator(consumerKey, token)
      val req = new NingWSRequest(client, "http://playframework.com/")
        .setHeader("Content-Type", "application/x-www-form-urlencoded") // set content type by hand
        .setBody("param1=value1")
        .setHeader("Content-Length", "9001") // add a meaningless content length here...
        .sign(calc)
        .asInstanceOf[NingWSRequest]
        .buildRequest()

      val headers = req.getHeaders
      headers.getFirstValue("Content-Length") must beNull // no content length!
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
  }

  def requestWithTimeout(timeout: Long) = {
    val client = mock[NingWSClient]
    val request = new NingWSRequest(client, "http://example.com")
    request.setRequestTimeout(timeout)
    request.buildRequest().getRequestTimeout()
  }
}
