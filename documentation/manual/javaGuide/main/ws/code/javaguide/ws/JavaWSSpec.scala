/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.ws

import org.specs2.mutable._
import play.api.test._

import play.api.mvc._
import play.api.libs.json._

import play.test.Helpers._

import play.api.test.FakeApplication
import play.api.libs.json.JsObject
import javaguide.testhelpers.MockJavaAction
import play.api.http.Status

object JavaWSSpec extends Specification with Results with Status {
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
      val result = MockJavaAction.call(new JavaWS.Controller1(), fakeRequest())

      status(result) must equalTo(OK)
    }

    "compose WS calls successfully" in new WithServer(app = fakeApplication, port = 3333) {
      val result = MockJavaAction.call(new JavaWS.Controller2(), fakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must beEqualTo("Number of comments: 10")
    }
  }

}
