/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.routing

import controllers.Assets
import org.specs2.mutable.Specification
import play.api.test.FakeRequest

object ScalaSirdRouter extends Specification {

  //#imports
  import play.api.mvc._
  import play.api.routing._
  import play.api.routing.sird._
  //#imports

  "sird router" should {
    "allow a simple match" in {
      //#simple
      val router = Router.from {
        case GET(p"/hello/$to") => Action {
          Results.Ok(s"Hello $to")
        }
      }
      //#simple

      router.routes.lift(FakeRequest("GET", "/hello/world")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/goodbye/world")) must beNone
    }

    "allow a full path match" in {
      //#full-path
      val router = Router.from {
        case GET(p"/assets/$file*") =>
          Assets.versioned(path = "/public", file = file)
      }
      //#full-path

      router.routes.lift(FakeRequest("GET", "/assets/javascripts/main.js")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/foo/bar")) must beNone
    }

    "allow a regex match" in {
      //#regexp
      val router = Router.from {
        case GET(p"/items/$id<[0-9]+>") => Action {
          Results.Ok(s"Item $id")
        }
      }
      //#regexp

      router.routes.lift(FakeRequest("GET", "/items/21")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items/foo")) must beNone
    }

    "allow sub extractor" in {
      //#int
      val router = Router.from {
        case GET(p"/items/${int(id)}") => Action {
          Results.Ok(s"Item $id")
        }
      }
      //#int

      router.routes.lift(FakeRequest("GET", "/items/21")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items/foo")) must beNone
    }

    "allow complex extractors" in {
      //#complex
      val router = Router.from {
        case rh @ GET(p"/items/${idString @ int(id)}")
          if rh.getQueryString("type").isDefined =>
          Action {
            Results.Ok(s"Item $id")
          }
      }
      //#complex

      router.routes.lift(FakeRequest("GET", "/items/21?type=json")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items/21")) must beNone
      router.routes.lift(FakeRequest("GET", "/items/foo?type=json")) must beNone
    }

  }


}
