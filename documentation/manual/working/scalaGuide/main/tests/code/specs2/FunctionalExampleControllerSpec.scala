/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

import play.api.test._
import scalaguide.tests.controllers

class FunctionalExampleControllerSpec extends PlaySpecification {
  // #scalafunctionaltest-functionalexamplecontrollerspec
  "respond to the index Action" in new WithApplication {
    override def running() = {
      val controller = app.injector.instanceOf[scalaguide.tests.controllers.HomeController]
      val result     = controller.index()(FakeRequest())

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/plain")
      contentAsString(result) must contain("Hello Bob")
    }
  }
  // #scalafunctionaltest-functionalexamplecontrollerspec
}
