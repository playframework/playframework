/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Futures

import scala.concurrent.ExecutionContext

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "HomeController GET" should {

    "responds 'original' in plain text" in {
      implicit val executionContext = inject[ExecutionContext]
      val futures = inject[Futures]

      val controller = new HomeController(stubControllerComponents(), app.actorSystem, app.coordinatedShutdown, futures)
      val home       = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
      contentAsString(home) must include("original")
    }

  }
}
