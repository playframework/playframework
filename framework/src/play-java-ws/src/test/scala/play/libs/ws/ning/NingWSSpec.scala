package play.libs.ws.ning

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito._

object NingWSSpec extends Specification with Mockito {

  "NingWSRequest" should {

    "should respond to getMethod" in {
      val client = mock[NingWSClient]
      val request : NingWSRequest = new NingWSRequest(client, "GET")
      request.getMethod must be_==("GET")
    }

  }

}
