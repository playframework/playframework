package play.api.libs.openid

import org.specs2.mock.Mockito
import play.api.http.{ContentTypeOf, Writeable, HeaderNames}
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import play.api.http.Status._
import scala.concurrent.Future


class WSMock extends Mockito {
    val request = mock[WSRequestHolder]
    val response = mock[Response]

    val urls:collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

    response.status returns OK
    response.header(HeaderNames.CONTENT_TYPE) returns Some("text/html;charset=UTF-8")
    response.body returns ""

    request.get() returns Future.successful(response)
    request.post(anyString)(any[Writeable[String]], any[ContentTypeOf[String]]) returns Future.successful(response)

    def url(url: String): WSRequestHolder = {
      urls += url
      request
    }
  }