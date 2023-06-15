/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc._
import play.api.routing.sird
import play.api.routing.Router
import play.api.test.ApplicationFactories
import play.api.test.PlaySpecification
import play.api.BuiltInComponents
import play.core.server.ServerEndpoint
import play.it.test._

class UriHandlingSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport
    with ApplicationFactories {
  private def makeRequest[T: AsResult](path: String, skipAkkaHttps2: Boolean = false)(
      block: (ServerEndpoint, okhttp3.Response) => T
  ): Fragment =
    withRouter { (components: BuiltInComponents) =>
      import components.{ defaultActionBuilder => Action }
      import sird.UrlContext
      Router.from {
        case sird.GET(p"/path") =>
          Action { (request: Request[_]) => Results.Ok(request.queryString) }
        case _ =>
          Action { (request: Request[_]) => Results.Ok(request.path + queryToString(request.queryString)) }
      }
    }.withAllOkHttpEndpoints { (okEndpoint: OkHttpEndpoint) =>
      if (
        skipAkkaHttps2 && okEndpoint.endpoint.scheme == "https" && okEndpoint.endpoint.description.contains(
          "Akka HTTP HTTP/2"
        )
      ) {
        skipped.asInstanceOf[T]
      } else {
        val response: okhttp3.Response = okEndpoint.call(path)
        block(okEndpoint.endpoint, response)
      }
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

    "handle '/pat?param=%_D%' as a URI with an invalid percent-encoded character in query string" in makeRequest(
      "/pat?param=%_D%",
      // TODO: Disabled the (secure) pekko-http2 test, an URI parsing bug causes requests to be stuck forever, never reaching Play:
      // https://github.com/apache/incubator-pekko-http/issues/59
      // https://github.com/akka/akka-http/issues/4226
      skipAkkaHttps2 = true
    ) {
      case (endpoint, response) => {
        response.code() must_=== 400
        response.body.string must beLike {
          case akkaHttpResponseBody
              if akkaHttpResponseBody == "Illegal request-target: Invalid input '_', expected HEXDIG (line 1, column 13)" =>
            ok // pekko-http responses directly, not even passing the request to Play, therefore there is no chance of a Play error handler to be called
          case nettyResponseBody if nettyResponseBody == "invalid hex byte '_D' at index 8 of '?param=%_D%'" =>
            ok // we made the netty backend response directly as well, also not going through the error handler, to stay on par with pekko-http
          case _ => ko
        }
      }
    }
  }
}
