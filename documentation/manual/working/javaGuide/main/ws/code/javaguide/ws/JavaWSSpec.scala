/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.ws

import org.specs2.mutable._
import play.api.test._

import play.api.mvc._
import play.api.libs.json._

import play.test.Helpers._

import play.api.test.FakeApplication
import play.api.libs.json.JsObject
import javaguide.testhelpers.MockJavaActionHelper
import play.api.http.Status

object JavaWSSpec extends Specification with Results with Status {
  // It's much easier to test this in Scala because we need to set up a
  // fake application with routes.

  def fakeApplication = FakeApplication(withRoutes = {
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
      val result = MockJavaActionHelper.call(app.injector.instanceOf[JavaWS.Controller1], fakeRequest())

      result.status() must equalTo(OK)
    }

    "compose WS calls successfully" in new WithServer(app = fakeApplication, port = 3333) {
      val result = MockJavaActionHelper.call(app.injector.instanceOf[JavaWS.Controller2], fakeRequest())

      result.status() must equalTo(OK)
      contentAsString(result) must beEqualTo("Number of comments: 10")
    }
  }

}
