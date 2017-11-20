/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import okhttp3.Response
import play.api.BuiltInComponents
import play.api.mvc._
import play.api.routing.{ Router, sird }
import play.api.test.PlaySpecification
import play.it.test._

class UriHandlingSpec extends PlaySpecification with EndpointIntegrationSpecification with OkHttpEndpointSupport {

  val app: ApplicationFactory = serveRouter { components: BuiltInComponents =>
    import components.{ defaultActionBuilder => Action }
    import sird.UrlContext
    Router.from {
      case sird.GET(p"/path") => Action { request: Request[_] => Results.Ok(request.queryString) }
      case _ => Action { request: Request[_] =>
        val combined = request.queryString.map { case (key, value) => key + "=" + value.sorted.mkString("|,|") }.mkString("&")
        val separator = if (combined.nonEmpty) "?" else ""
        Results.Ok(request.path + separator + combined)
      }
    }
  }

  def request(path: String): ServerEndpointOps[Response] = app.useOkHttp.request(path)

  "Server" should {
    "preserve order of repeated query string parameters" in
      request("/path?a=1&b=1&b=2&b=3&b=4&b=5")
      .forEndpoints(_.body.string must_== "a=1&b=1&b=2&b=3&b=4&b=5")

    "handle '/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}' as a valid URI" in
      request("/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}")
      .forEndpoints(_.body.string must_=== """/pat/resources/BodhiApplication?where={"name":"hsdashboard"}""")

    "handle '/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0' as a URI" in
      request("/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0")
      .forEndpoints(_.body.string must_=== """/dynatable/?queries[search]={"condition":"AND","rules":[]}&page=1&perPage=10&offset=0""")

    "handle '/foo%20bar.txt' as a URI" in
      request("/foo%20bar.txt")
      .forEndpoints(_.body.string must_=== """/foo%20bar.txt""")

    "handle '/?filter=a&filter=b' as a URI" in
      request("/?filter=a&filter=b")
      .forEndpoints(_.body.string must_=== """/?filter=a|,|b""")

    "handle '/?filter=a,b' as a URI" in
      request("/?filter=a,b")
      .forEndpoints(_.body.string must_=== """/?filter=a,b""")
  }

}
