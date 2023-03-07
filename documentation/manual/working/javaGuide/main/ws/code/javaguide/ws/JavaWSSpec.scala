/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws

import javaguide.testhelpers.MockJavaActionHelper
import javaguide.ws.JavaWS.Controller3
import javaguide.ws.JavaWS.Controller4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.slf4j.Logger
import org.specs2.mutable._
import play.api.{ Application => PlayApplication }
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.mvc._
import play.api.test._
import play.test.Helpers._

object JavaWSSpec extends Specification with Results with Status {
  // It's much easier to test this in Scala because we need to set up a
  // fake application with routes.

  def fakeApplication: PlayApplication =
    GuiceApplicationBuilder()
      .appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        ({
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
        })
      }
      .build()

  "The Java WSClient" should {
    "call WS correctly" in (new WithServer(app = fakeApplication, port = 3333) {
      override def running() = {
        val result = MockJavaActionHelper.call(app.injector.instanceOf[JavaWS.Controller1], fakeRequest())

        result.status() must equalTo(OK)
      }
    })()

    "compose WS calls successfully" in (new WithServer(app = fakeApplication, port = 3333) {
      override def running() = {
        val result = MockJavaActionHelper.call(app.injector.instanceOf[JavaWS.Controller2], fakeRequest())

        result.status() must equalTo(OK)
        contentAsString(result) must beEqualTo("Number of comments: 10")
      }
    })()

    "call WS with a filter" in (new WithServer(app = fakeApplication, port = 3333) {
      override def running() = {
        val controller = app.injector.instanceOf[Controller3]
        val logger     = mock(classOf[Logger])
        controller.setLogger(logger)

        val result = MockJavaActionHelper.call(controller, fakeRequest())

        result.status() must equalTo(OK)
        verify(logger).debug("url = http://localhost:3333/feed")
      }
    })()

    "call WS with a timeout" in (new WithServer(app = fakeApplication) {
      override def running() = {
        val controller = app.injector.instanceOf[Controller4]

        val result = MockJavaActionHelper.call(controller, fakeRequest())

        contentAsString(result) must beEqualTo("Timeout after 1 second")
      }
    })()
  }
}
