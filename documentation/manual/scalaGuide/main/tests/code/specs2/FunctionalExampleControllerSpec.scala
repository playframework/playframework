<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/FunctionalExampleControllerSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests
=======
package scalaguide.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/FunctionalExampleControllerSpec.scala

import controllers._

import play.api.test._
import play.api.test.Helpers._

object FunctionalExampleControllerSpec extends PlaySpecification {

  // #scalafunctionaltest-functionalexamplecontrollerspec
  "respond to the index Action" in {
    val result = controllers.Application.index()(FakeRequest())

    status(result) must equalTo(OK)
    contentType(result) must beSome("text/plain")
    contentAsString(result) must contain("Hello Bob")
  }
  // #scalafunctionaltest-functionalexamplecontrollerspec
}
