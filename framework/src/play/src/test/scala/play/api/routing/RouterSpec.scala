/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing

import org.specs2.mutable.Specification
import play.api.mvc.Handler
import play.api.routing.Router.Routes
import play.api.routing.sird._
import play.core.test.FakeRequest

class RouterSpec extends Specification {
  "Routers" should {
    object First extends Handler
    object Second extends Handler
    object Third extends Handler
    val firstRouter = Router.from {
      case GET(p"/oneRoute") => First
    }
    val secondRouter = Router.from {
      case GET(p"/anotherRoute") => Second
    }
    val thirdRouter = Router.from {
      case GET(p"/oneRoute") => Third // sic, same route as in firstRouter
    }

    "be composable" in {
      "find handler from first router" in {
        firstRouter.orElse(secondRouter).handlerFor(FakeRequest("GET", "/oneRoute")) must be some (First)
      }
      "find handler from second router" in {
        firstRouter.orElse(secondRouter).handlerFor(FakeRequest("GET", "/anotherRoute")) must be some (Second)
      }
      "none when handler is not present in any of the routers" in {
        firstRouter.orElse(secondRouter).handlerFor(FakeRequest("GET", "/noSuchRoute")) must beNone
      }
      "prefer first router if both match" in {
        firstRouter.orElse(thirdRouter).handlerFor(FakeRequest("GET", "/oneRoute")) must be some (First)
      }
      "withPrefix should be applied recursively" in {
        val r1 = firstRouter.withPrefix("/stan")
        val r2 = secondRouter.withPrefix("/kyle")
        val r3 = r1.orElse(r2).withPrefix("/cartman")
        r3.handlerFor(FakeRequest("GET", "/cartman/stan/oneRoute")) must be some (First)
        r3.handlerFor(FakeRequest("GET", "/cartman/kyle/anotherRoute")) must be some (Second)
      }
      "documentation should be concatenated" in {
        case class DocRouter(documentation: Seq[(String, String, String)]) extends Router {
          def routes: Routes = PartialFunction.empty
          def withPrefix(prefix: String): Router = this
        }

        val r1 = DocRouter(Seq(("Jesse", "Walter", "Skyler")))
        val r2 = DocRouter(Seq(("Gus", "Tuco", "Lydia")))
        r1.orElse(r2).documentation must beEqualTo(Seq(("Jesse", "Walter", "Skyler"), ("Gus", "Tuco", "Lydia")))
      }
    }
  }
}
