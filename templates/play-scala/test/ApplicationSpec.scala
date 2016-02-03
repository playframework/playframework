import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Routes" should {

    "send 404 for an unknown resource" in new WithApplication {
      route(app, FakeRequest(GET, "/boum")) must beSome.which (status(_) == NOT_FOUND)
    }

  }

  "HomeController" should {

    "render the index page" in new WithApplication {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Your new application is ready.")
    }

  }

  "CountController" should {

    "return an increasing count" in new WithApplication {
      contentAsString(route(FakeRequest(GET, "/count")).get) must_== ("0")
      contentAsString(route(FakeRequest(GET, "/count")).get) must_== ("1")
      contentAsString(route(FakeRequest(GET, "/count")).get) must_== ("2")
    }

  }

}
