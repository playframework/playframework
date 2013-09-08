package scalaguide.tests

import controllers._

import play.api.test._
import play.api.test.Helpers._

// #scalatest-functionalexamplecontrollerspec
object FunctionalExampleControllerSpec extends PlaySpecification {

  "respond to the index Action" in new WithApplication {
    val result = controllers.Application.index()(FakeRequest())

    status(result) must equalTo(OK)
    contentType(result) must beSome("text/plain")
    contentAsString(result) must contain("Hello Bob")
  }
}
// #scalatest-functionalexamplecontrollerspec
