package play.api.test

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test.Helpers._

object FakesSpec extends Specification {

  "FakeApplication" should {

    "allow adding routes inline" in {
      val app = new FakeApplication(
        withRoutes = {
          case ("GET", "/inline") => Action { Results.Ok("inline route") }
        }
      )
      running(app) {
        val result = route(app, FakeRequest("GET", "/inline"))
        result must beSome
        contentAsString(result.get) must_== "inline route"
        route(app, FakeRequest("GET", "/foo")) must beNone
      }
    }
  }

}
