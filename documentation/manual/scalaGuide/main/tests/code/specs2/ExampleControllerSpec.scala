package scalaguide.tests.specs

import play.api.mvc._
import play.api.test._
import scala.concurrent.Future

// #scalatest-examplecontroller
trait ExampleController {
  this: Controller =>

  def index() = Action {
    Ok("ok")
  }
}

object ExampleController extends Controller with ExampleController
// #scalatest-examplecontroller

// #scalatest-examplecontrollerspec
object ExampleControllerSpec extends PlaySpecification with Results {

  class TestController() extends Controller with ExampleController

  "Example Page#index" should {
    "should be valid" in {
      val controller = new TestController()
      val result: Future[SimpleResult] = controller.index().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "ok"
    }
  }
}
// #scalatest-examplecontrollerspec
