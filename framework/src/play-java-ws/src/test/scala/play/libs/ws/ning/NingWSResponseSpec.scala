package play.libs.ws.ning

import scala.collection.JavaConverters._

import org.specs2.mock.Mockito
import org.specs2.mutable._

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, Response }

object NingWSResponseSpec extends Specification with Mockito {

  private val emptyMap = new java.util.HashMap[String, java.util.Collection[String]]

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

    "getUnderlying" in {
      pending
    }

    /*
    getUnderlying
    getStatus
    getStatusText
    getAllHeaders
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

}
