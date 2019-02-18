/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws

import org.specs2.mutable._
import play.api.test._
import play.api.mvc._
import play.api.libs.json._
import play.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import javaguide.testhelpers.MockJavaActionHelper
import javaguide.ws.JavaWS.Controller3
import javaguide.ws.JavaWS.Controller4

import org.slf4j.Logger
import org.specs2.mock.Mockito
import play.api.http.Status
import play.api.{ Application => PlayApplication }

object JavaWSSpec extends Specification with Results with Status with Mockito {
  // It's much easier to test this in Scala because we need to set up a
  // fake application with routes.

  def fakeApplication: PlayApplication =
    GuiceApplicationBuilder()
      .routes {
        case ("GET", "/feed") =>
          Action {
            val obj: JsObject = Json.obj(
              "title"       -> "foo",
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
      }
      .build()

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

    "call WS with a filter" in new WithServer(app = fakeApplication, port = 3333) {
      val controller = app.injector.instanceOf[Controller3]
      val logger     = mock[Logger]
      controller.setLogger(logger)

      val result = MockJavaActionHelper.call(controller, fakeRequest())

      result.status() must equalTo(OK)
      there.was(one(logger).debug("url = http://localhost:3333/feed"))
    }

    "call WS with a timeout" in new WithServer(app = fakeApplication) {
      val controller = app.injector.instanceOf[Controller4]

      val result = MockJavaActionHelper.call(controller, fakeRequest())

      contentAsString(result) must beEqualTo("Timeout after 1 second")
    }
  }

}
