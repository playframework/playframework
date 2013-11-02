package play.it.ws

import com.ning.http.client.AsyncHttpClient

import play.api.test._
import play.api.libs.ws._

object WsSpec extends PlaySpecification {

  "support timeout configuration" in {

    "no timeout specified" in withApplication() {
      WS.newClient() must beAnInstanceOf[AsyncHttpClient]
    }

    "simple timeout specified" in withApplication("ws.timeout" -> 60000) {
      WS.newClient() must beAnInstanceOf[AsyncHttpClient]
    }

    "detailed timeout specified" in withApplication(
      "ws.timeout.connection" -> 60000,
      "ws.timeout.idle" -> 60000,
      "ws.timeout.request" -> 60000)
    {
      WS.newClient() must beAnInstanceOf[AsyncHttpClient]
    }

  }

  def withApplication[T](config: (String, Any)*)(block: => T): T = running(
    FakeApplication(additionalConfiguration = Map(config:_ *))
  )(block)

}
