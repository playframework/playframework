package play.api.libs.openid

import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import org.specs2.mock.Mockito
import play.api.http.Status._
import play.api.http.HeaderNames

class WSMock extends Mockito {
  val request = mock[WSRequestHolder]
  val response = mock[Response]

  response.status returns OK
  response.header(HeaderNames.CONTENT_TYPE) returns Some("text/html;charset=UTF-8")
  response.body returns ""

  request.get() returns Promise.pure(response)

  def url(url: String): WSRequestHolder = request
}
