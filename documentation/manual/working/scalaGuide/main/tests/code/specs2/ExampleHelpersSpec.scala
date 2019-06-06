/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package specs2

import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.test.Helpers
import play.api.test.Injecting
import play.api.test.PlaySpecification
import play.api.test.WithApplication

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

  class MyController(cc: ControllerComponents) extends AbstractController(cc) {}
}
