/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package specs2

import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import play.api.test.{Helpers, Injecting, PlaySpecification, WithApplication}

import scala.concurrent.ExecutionContext

/**
 * Snippets to show off test helpers
 */
class ExampleHelpersSpec extends PlaySpecification {

  // #scalafunctionaltest-noinjecting
  "test" in new WithApplication() {
    val executionContext = app.injector.instanceOf[ExecutionContext]
    executionContext must beAnInstanceOf[ExecutionContext]
  }
  // #scalafunctionaltest-noinjecting

  // #scalafunctionaltest-injecting
  "test" in new WithApplication() with play.api.test.Injecting {
    val executionContext = inject[ExecutionContext]
    executionContext must beAnInstanceOf[ExecutionContext]
  }
  // #scalafunctionaltest-injecting

  // #scalatest-stubbodyparser
  val stubParser = Helpers.stubBodyParser(AnyContent("hello"))
  // #scalatest-stubbodyparser

  // #scalatest-stubcontrollercomponents
  val controller = new MyController(
    Helpers.stubControllerComponents(bodyParser = stubParser)
  )
  // #scalatest-stubcontrollercomponents

  class MyController(cc: ControllerComponents) extends AbstractController(cc) {

  }
}
