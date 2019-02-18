/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.specs2

import scalaguide.tests.controllers

import play.api.test._

class FunctionalExampleControllerSpec extends PlaySpecification {

  // #scalafunctionaltest-functionalexamplecontrollerspec
  "respond to the index Action" in new WithApplication {
    val controller = app.injector.instanceOf[scalaguide.tests.controllers.HomeController]
    val result     = controller.index()(FakeRequest())

    status(result) must equalTo(OK)
    contentType(result) must beSome("text/plain")
    contentAsString(result) must contain("Hello Bob")
  }
  // #scalafunctionaltest-functionalexamplecontrollerspec
}
