package play.api.routing

import org.specs2.mutable.Specification
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.routing.sird._
import play.core.test.FakeRequest

object SimpleRouterSpec extends Specification {

  "SimpleRouter" should {

    val RootAction = Action(Ok)

    val router = SimpleRouter {
      case GET(p"/") => RootAction
    }

    "support root requests" in {

      "without prefix" in {
        router.handlerFor(FakeRequest("GET", "/")) must beLike {
          case Some(RootAction) => ok
        }
      }

      "with prefix" in {
        val prefixedRouter = router.withPrefix("/prefix")

        prefixedRouter.handlerFor(FakeRequest("GET", "/prefix")) must beLike {
          case Some(RootAction) => ok
        }

        prefixedRouter.handlerFor(FakeRequest("GET", "/prefix/")) must beLike {
          case Some(RootAction) => ok
        }
      }

    }
  }

}
