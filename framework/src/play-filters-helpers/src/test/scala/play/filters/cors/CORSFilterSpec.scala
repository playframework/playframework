/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import javax.inject.Inject

import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.mvc.{ Action, DefaultActionBuilder, Results }
import play.api.routing.sird._
import play.api.routing.{ Router, SimpleRouterImpl }
import play.filters.cors.CORSFilterSpec._

object CORSFilterSpec {

  class Filters @Inject() (corsFilter: CORSFilter) extends HttpFilters {
    def filters = Seq(corsFilter)
  }

  class CorsApplicationRouter @Inject() (action: DefaultActionBuilder) extends SimpleRouterImpl({
    case p"/error" => action { req => throw sys.error("error") }
    case _ => Action(Results.Ok)
  })

}

class CORSFilterSpec extends CORSCommonSpec {

  def withApplication[T](conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T = {
    running(_.configure(conf).overrides(
      bind[Router].to[CorsApplicationRouter],
      bind[HttpFilters].to[Filters]
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
