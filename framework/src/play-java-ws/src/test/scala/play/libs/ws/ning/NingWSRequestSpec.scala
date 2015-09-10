package play.libs.ws.ning

import org.specs2.mock.Mockito
import org.specs2.mutable._

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