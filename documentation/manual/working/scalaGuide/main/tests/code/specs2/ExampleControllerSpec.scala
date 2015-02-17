/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.specs2

// #scalatest-examplecontrollerspec
import play.api.mvc._
import play.api.test._
import scala.concurrent.Future

object ExampleControllerSpec extends PlaySpecification with Results {

  class TestController() extends Controller with ExampleController

  "Example Page#index" should {
    "should be valid" in {
      val controller = new TestController()
      val result: Future[Result] = controller.index().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "ok"
    }
  }
}
// #scalatest-examplecontrollerspec

// #scalatest-examplecontroller
trait ExampleController {
  this: Controller =>

  def index() = Action {
    Ok("ok")
  }
}

object ExampleController extends Controller with ExampleController
// #scalatest-examplecontroller
