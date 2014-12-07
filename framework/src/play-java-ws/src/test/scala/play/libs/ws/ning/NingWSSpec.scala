package play.libs.ws.ning

import scala.collection.JavaConverters._

import org.specs2.mock.Mockito
import org.specs2.mutable._

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, Response }

object NingWSSpec extends Specification with Mockito {

  val emptyMap = new java.util.HashMap[String, java.util.Collection[String]]

  "NingWSRequestHolder" should {

    "set virtualHost correctly" in {
      val client = mock[NingWSClient]
      val holder = new NingWSRequestHolder(client, "http://example.com")
      holder.setVirtualHost("foo.com")
      val actual = holder.getVirtualHost
      actual must beEqualTo("foo.com")
    }

  }

  "NingWSRequest" should {

    "should respond to getMethod" in {
      val client = mock[NingWSClient]
      val request: NingWSRequest = new NingWSRequest(client, "GET", "http://example.com", emptyMap, emptyMap)
      request.getMethod must be_==("GET")
    }

    "should set virtualHost appropriately" in {
      val client = mock[NingWSClient]
      val request = new NingWSRequest(client, "GET", "http://example.com", emptyMap, emptyMap)
      request.setVirtualHost("foo.com")
      val actual = request.getBuilder().build().getVirtualHost()
      actual must beEqualTo("foo.com")
    }

  }

  "NingWSResponse" should {

    "should get headers map which retrieves headers case insensitively" in {
      val srcResponse = mock[Response]
      val srcHeaders = new FluentCaseInsensitiveStringsMap()
        .add("Foo", "a")
        .add("foo", "b")
        .add("FOO", "b")
        .add("Bar", "baz")
      srcResponse.getHeaders returns srcHeaders
      val response = new NingWSResponse(srcResponse)
      val headers = response.getAllHeaders
      headers.get("foo").asScala must_== Seq("a", "b", "b")
      headers.get("BAR").asScala must_== Seq("baz")
    }

  }

}
