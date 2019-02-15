/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.BuiltInComponents
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird
import play.api.test.{ ApplicationFactories, PlaySpecification }
import play.core.server.ServerEndpoint
import play.it.test._

class UriHandlingSpec extends PlaySpecification with EndpointIntegrationSpecification with OkHttpEndpointSupport with ApplicationFactories {

  private def makeRequest[T: AsResult](path: String)(block: (ServerEndpoint, okhttp3.Response) => T): Fragment = withRouter { components: BuiltInComponents =>
    import components.{ defaultActionBuilder => Action }
    import sird.UrlContext
    Router.from {
      case sird.GET(p"/path") => Action { request: Request[_] => Results.Ok(request.queryString) }
      case _ => Action { request: Request[_] => Results.Ok(request.path + queryToString(request.queryString)) }
    }
  }.withAllOkHttpEndpoints { okEndpoint: OkHttpEndpoint =>
    val response: okhttp3.Response = okEndpoint.call(path)
    block(okEndpoint.endpoint, response)
  }

  private def queryToString(qs: Map[String, Seq[String]]) = {
    val queryString = qs.map { case (key, value) => key + "=" + value.sorted.mkString("|,|") }.mkString("&")
    if (queryString.nonEmpty) "?" + queryString else ""
  }

  "Server" should {

    "preserve order of repeated query string parameters" in makeRequest(
      "/path?a=1&b=1&b=2&b=3&b=4&b=5"
    ) {
        case (endpoint, response) => {
          response.body.string must_== "a=1&b=1&b=2&b=3&b=4&b=5"
        }
      }

    "handle '/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}' as a valid URI" in makeRequest(
      "/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}"
    ) {
        case (endpoint, response) => {
          response.body.string must_=== """/pat/resources/BodhiApplication?where={"name":"hsdashboard"}"""
        }
      }

    "handle '/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0' as a URI" in makeRequest(
      "/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0"
    ) {
        case (endpoint, response) => {
          response.body.string must_=== """/dynatable/?queries[search]={"condition":"AND","rules":[]}&page=1&perPage=10&offset=0"""
        }
      }

    "handle '/foo%20bar.txt' as a URI" in makeRequest(
      "/foo%20bar.txt"
    ) {
        case (endpoint, response) =>
          response.body.string must_=== """/foo%20bar.txt"""
      }

    "handle '/?filter=a&filter=b' as a URI" in makeRequest(
      "/?filter=a&filter=b"
    ) {
        case (endpoint, response) => {
          response.body.string must_=== """/?filter=a|,|b"""
        }
      }

    "handle '/?filter=a,b' as a URI" in makeRequest(
      "/?filter=a,b"
    ) {
        case (endpoint, response) => {
          response.body.string must_=== """/?filter=a,b"""
        }
      }

    "handle '/pat?param=%_D%' as a URI with an invalid query string" in makeRequest(
      "/pat?param=%_D%"
    ) {
        case (endpoint, response) => {
          response.body.string must_=== """/pat"""
        }
      }
  }

}
