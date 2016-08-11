import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

/**
 * A Person Controller specification
 */
class PersonControllerSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "PersonController" should {

    "render the index page" in {
      val index = route(app, FakeRequest(GET, "/")).get

      status(index) mustBe OK
      contentType(index) mustBe Some("text/html")
      contentAsString(index) must include ("Add Person")
    }

    "post and create a new user" in  {
      route(app, FakeRequest(POST, "/persons").withFormUrlEncodedBody("name" -> "Play", "age" -> "5")).map(status(_)) mustBe Some(SEE_OTHER)
    }

    "post and receive an error page" in  {
      route(app, FakeRequest(POST, "/persons").withFormUrlEncodedBody("name" -> "Play", "age" -> "-1")).map(status(_)) mustBe Some(BAD_REQUEST)
    }

  }

}
