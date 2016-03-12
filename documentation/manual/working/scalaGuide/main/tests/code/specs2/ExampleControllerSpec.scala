/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.specs2

// #scalatest-examplecontrollerspec
import play.api.mvc._
import play.api.test._
import scala.concurrent.Future

object ExampleControllerSpec extends PlaySpecification with Results {

  "Example Page#index" should {
    "should be valid" in {
      val controller = new ExampleController()
      val result: Future[Result] = controller.index().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "ok"
    }
  }
}
// #scalatest-examplecontrollerspec

// #scalatest-examplecontroller
class ExampleController extends Controller {
  def index() = Action {
    Ok("ok")
  }
}
// #scalatest-examplecontroller
