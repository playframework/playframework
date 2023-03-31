/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package specs2

import scala.concurrent.ExecutionContext

import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.test.Helpers
import play.api.test.Injecting
import play.api.test.PlaySpecification
import play.api.test.WithApplication

/**
 * Snippets to show off test helpers
 */
class ExampleHelpersSpec extends PlaySpecification {
  // #scalafunctionaltest-noinjecting
  "test" in new WithApplication() {
    override def running() = {
      val executionContext = app.injector.instanceOf[ExecutionContext]
      executionContext must beAnInstanceOf[ExecutionContext]
    }
  }
  // #scalafunctionaltest-noinjecting

  // #scalafunctionaltest-injecting
  "test" in new WithApplication() with play.api.test.Injecting {
    override def running() = {
      val executionContext = inject[ExecutionContext]
      executionContext must beAnInstanceOf[ExecutionContext]
    }
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
