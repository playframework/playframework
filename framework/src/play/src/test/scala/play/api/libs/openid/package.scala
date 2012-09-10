package play.api.libs

import io.Source
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import java.net.{MalformedURLException, URL}
import util.control.Exception._
import collection.JavaConverters._
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import org.specs2.mock.Mockito
import play.api.http.Status._
import play.api.http.{ContentTypeOf, Writeable, HeaderNames}

package object openid {
  type Params = Map[String, Seq[String]]

  implicit def stringToSeq(s: String): Seq[String] = Seq(s)

  trait RichUrl[A] {
    def hostAndPath: String
  }

  implicit def urlToRichUrl(url: URL) = new RichUrl[URL] {
    def hostAndPath = new URL(url.getProtocol, url.getHost, url.getPort, url.getPath).toExternalForm
  }

  def readFixture(filePath: String) = this.synchronized {
    Source.fromInputStream(this.getClass.getResourceAsStream(filePath)).mkString
  }

  def parseQueryString(url: String): Params = {
    catching(classOf[MalformedURLException]) opt new URL(url) map {
      url =>
        new QueryStringDecoder(url.toURI.getRawQuery, false).getParameters.asScala.mapValues(_.asScala.toSeq).toMap
    } getOrElse Map()
  }

  // See 10.1 - Positive Assertions
  // http://openid.net/specs/openid-authentication-2_0.html#positive_assertions
  def createDefaultResponse(claimedId: String,
                            identity: String,
                            defaultSigned: String = "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle"): Map[String, Seq[String]] = Map(
    "openid.ns" -> "http://specs.openid.net/auth/2.0",
    "openid.mode" -> "id_res",
    "openid.op_endpoint" -> "https://www.google.com/a/example.com/o8/ud?be=o8",
    "openid.claimed_id" -> claimedId,
    "openid.identity" -> identity,
    "openid.return_to" -> "https://example.com/openid?abc=false",
    "openid.response_nonce" -> "2012-05-25T06:47:55ZEJvRv76xQcWbTG",
    "openid.assoc_handle" -> "AMlYA9VC8_UIj4-y4K_X2E_mdv-123-ABC",
    "openid.signed" -> defaultSigned,
    "openid.sig" -> "MWRsJZ/9AOMQt9gH6zTZIfIjk6g="
  )


  class WSMock extends Mockito {
    val request = mock[WSRequestHolder]
    val response = mock[Response]

    val urls:collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

    response.status returns OK
    response.header(HeaderNames.CONTENT_TYPE) returns Some("text/html;charset=UTF-8")
    response.body returns ""

    request.get() returns Promise.pure(response)
    request.post(anyString)(any[Writeable[String]], any[ContentTypeOf[String]]) returns Promise.pure(response)

    def url(url: String): WSRequestHolder = {
      urls += url
      request
    }
  }

}