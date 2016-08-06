/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import javax.inject.Inject

import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.mvc.{ Action, Results }
import play.api.routing.Router
import play.api.routing.sird._

object CORSFilterSpec {

  class Filters @Inject()(corsFilter: CORSFilter) extends HttpFilters {
    def filters = Seq(corsFilter)
  }

}

class CORSFilterSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T = {
    running(_.configure(conf).overrides(
      bind[Router].to(Router.from {
        case p"/error" => Action { req => throw sys.error("error") }
        case _ => Action(Results.Ok)
      }),
      bind[HttpFilters].to[CORSFilterSpec.Filters]
    ))(block)
  }

  "The CORSFilter" should {

    val restrictPaths = Map("play.filters.cors.pathPrefixes" -> Seq("/foo", "/bar"))

    "pass through a cors request that doesn't match the path prefixes" in withApplication(conf = restrictPaths) { app =>
      val result = route(app, fakeRequest("GET", "/baz").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    commonTests
  }
}