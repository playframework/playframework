package javaguide.ws

import org.specs2.mutable._
import play.api.test._

import play.api.mvc._
import play.api.libs.json._

import play.api.test.Helpers._

import play.api.test.FakeApplication
import play.api.libs.json.JsObject

object JavaWSSpec extends Specification with Results {
  // It's much easier to test this in Scala because we need to set up a
  // fake application with routes.

  val fakeApplication = FakeApplication(withRoutes = {
    case ("GET", "/feed") =>
      Action {
        val obj: JsObject = Json.obj(
          "title" -> "foo",
          "commentsUrl" -> "http://localhost:3333/comments"
        )
        Ok(obj)
      }
    case ("GET", "/comments") =>
         Action {
           val obj: JsObject = Json.obj(
             "count" -> "10"
           )
           Ok(obj)
         }
    case (_, _) =>
      Action {
        BadRequest("no binding found")
      }
  })

  "The Java WS class" should {
    "call WS correctly" in new WithServer(app = fakeApplication, port = 3333) {
      val result= JavaWS.Controller1.index().get(5000).getWrappedResult

      status(result) must equalTo(OK)
    }

    "compose WS calls successfully" in new WithServer(app = fakeApplication, port = 3333) {
      val result = JavaWS.Controller2.index().get(5000).getWrappedResult

      status(result) must equalTo(OK)
      contentAsString(result) must beEqualTo("Number of comments: 10")
    }
  }

}
