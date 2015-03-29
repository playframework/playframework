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

  }

}
