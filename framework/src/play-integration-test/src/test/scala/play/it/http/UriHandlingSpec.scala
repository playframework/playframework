/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.test.{ PlaySpecification, WsTestClient }
import play.it.{ AkkaHttpIntegrationSpecification, NettyIntegrationSpecification, ServerIntegrationSpecification }

import scala.util.Try

class NettyUriHandlingSpec extends UriHandlingSpec with NettyIntegrationSpecification
class AkkaHttpUriHandlingSpec extends UriHandlingSpec with AkkaHttpIntegrationSpecification

trait UriHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  def tryRequest[T](uri: String, result: Request[_] => Result)(block: Try[WSResponse] => T) = withServer(result) { implicit port =>
    val response = Try(await(wsUrl(uri).get()))
    block(response)
  }

  def queryToString(qs: Map[String, Seq[String]]) = {
    val queryString = qs.map { case (key, value) => key + "=" + value.sorted.mkString("|,|") }.mkString("&")
    if (queryString.nonEmpty) "?" + queryString else ""
  }

  def makeRequest[T](uri: String)(block: WSResponse => T) = {
    tryRequest(uri, request => Results.Ok(request.path + queryToString(request.queryString)))(tryResult => block(tryResult.get))
  }

  def withServer[T](result: Request[_] => Result, errorHandler: HttpErrorHandler = DefaultHttpErrorHandler)(block: play.api.test.Port => T) = {
    val port = testServerPort
    val app = GuiceApplicationBuilder()
      .overrides(bind[HttpErrorHandler].to(errorHandler))
      .routes { case _ => ActionBuilder.ignoringBody { request: Request[_] => result(request) } }
      .build()
    running(TestServer(port, app)) {
      block(port)
    }
  }

  "Server" should {
    "handle '/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}' as a valid URI" in makeRequest(
      "/pat/resources/BodhiApplication?where={%22name%22:%22hsdashboard%22}"
    ) { response =>
        response.body must_=== """/pat/resources/BodhiApplication?where={"name":"hsdashboard"}"""
      }
    "handle '/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0' as a URI" in makeRequest(
      "/dynatable/?queries%5Bsearch%5D=%7B%22condition%22%3A%22AND%22%2C%22rules%22%3A%5B%5D%7D&page=1&perPage=10&offset=0"
    ) { response =>
        response.body must_=== """/dynatable/?queries[search]={"condition":"AND","rules":[]}&page=1&perPage=10&offset=0"""
      }
    "handle '/foo%20bar.txt' as a URI" in makeRequest(
      "/foo%20bar.txt"
    ) { response =>
        response.body must_=== """/foo%20bar.txt"""
      }
    "handle '/?filter=a&filter=b' as a URI" in makeRequest(
      "/?filter=a&filter=b"
    ) { response =>
        response.body must_=== """/?filter=a|,|b"""
      }
    "handle '/?filter=a,b' as a URI" in makeRequest(
      "/?filter=a,b"
    ) { response =>
        response.body must_=== """/?filter=a,b"""
      }
  }

}
