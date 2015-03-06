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

    "allow extracting required query parameters" in {
      //#required
      val router = Router.from {
        case GET(p"/search" ? q"query=$query") => Action {
          Results.Ok(s"Searching for $query")
        }
      }
      //#required

      router.routes.lift(FakeRequest("GET", "/search?query=foo")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/search")) must beNone
    }

    "allow extracting optional query parameters" in {
      //#optional
      val router = Router.from {
        case GET(p"/items" ? q_o"page=$page") => Action {
          val thisPage = page.getOrElse("1")
          Results.Ok(s"Showing page $thisPage")
        }
      }
      //#optional

      router.routes.lift(FakeRequest("GET", "/items?page=10")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items")) must beSome[Handler]
    }

    "allow extracting multi value query parameters" in {
      //#many
      val router = Router.from {
        case GET(p"/items" ? q_s"tag=$tags") => Action {
          val allTags = tags.mkString(", ")
          Results.Ok(s"Showing items tagged: $allTags")
        }
      }
      //#many

      router.routes.lift(FakeRequest("GET", "/items?tag=a&tag=b")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items")) must beSome[Handler]
    }

    "allow extracting multiple query parameters" in {
      //#multiple
      val router = Router.from {
        case GET(p"/items" ? q_o"page=$page"
          & q_o"per_page=$perPage") => Action {
          val thisPage = page.getOrElse("1")
          val pageLength = perPage.getOrElse("10")

          Results.Ok(s"Showing page $thisPage of length $pageLength")
        }
      }
      //#multiple

      router.routes.lift(FakeRequest("GET", "/items?page=10&per_page=20")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items")) must beSome[Handler]
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

    "allow sub extractor on a query parameter" in {
      //#query-int
      val router = Router.from {
        case GET(p"/items" ? q_o"page=${int(page)}") => Action {
          val thePage = page.getOrElse(1)
          Results.Ok(s"Items page $thePage")
        }
      }
      //#query-int

      router.routes.lift(FakeRequest("GET", "/items?page=21")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items?page=foo")) must beNone
      router.routes.lift(FakeRequest("GET", "/items")) must beSome[Handler]
    }

    "allow complex extractors" in {
      //#complex
      val router = Router.from {
        case rh @ GET(p"/items/${idString @ int(id)}" ?
          q"price=${int(price)}")
          if price > 200 =>
          Action {
            Results.Ok(s"Expensive item $id")
          }
      }
      //#complex

      router.routes.lift(FakeRequest("GET", "/items/21?price=400")) must beSome[Handler]
      router.routes.lift(FakeRequest("GET", "/items/21?price=foo")) must beNone
      router.routes.lift(FakeRequest("GET", "/items/foo?price=400")) must beNone
    }

  }


}
