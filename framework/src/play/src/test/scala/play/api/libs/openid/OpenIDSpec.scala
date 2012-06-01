package play.api.libs.openid

import org.specs2.mutable.Specification
import scala.Predef._
import org.specs2.mock.Mockito
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import play.api.http.Status._

object OpenIDSpec extends Specification with Mockito {

  "OpenID" should {
    "initiate discovery" in {
      val ws = new WSMock
      val openId = new OpenIDClient(ws.url)
      openId.redirectURL("http://example.com", "http://foo.bar.com/openid")
      there was one(ws.request).get()
    }
  }

}
