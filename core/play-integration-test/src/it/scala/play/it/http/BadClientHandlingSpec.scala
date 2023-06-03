/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import scala.concurrent.Future
import scala.util.Random

import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing._
import play.api.test._
import play.filters.HttpFiltersComponents
import play.it._

class NettyBadClientHandlingSpec    extends BadClientHandlingSpec with NettyIntegrationSpecification
class AkkaHttpBadClientHandlingSpec extends BadClientHandlingSpec with AkkaHttpIntegrationSpecification

trait BadClientHandlingSpec extends PlaySpecification with ServerIntegrationSpecification {
  "Play" should {
    def withServer[T](errorHandler: HttpErrorHandler = DefaultHttpErrorHandler)(block: Port => T) = {
      val app = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple()))
        with HttpFiltersComponents {
        def router = {
          import sird._
          Router.from {
            case sird.POST(p"/action" ? q_o"query=$query") =>
              Action { request => Results.Ok(query.getOrElse("_")) }
            case _ =>
              Action {
                Results.Ok
              }
          }
        }
        override lazy val httpErrorHandler = errorHandler
      }.application

      runningWithPort(TestServer(testServerPort, app)) { port =>
        block(port)
      }
    }

    "gracefully handle long urls and return 414" in withServer() { port =>
      val url = new String(Random.alphanumeric.take(5 * 1024).toArray)

      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/" + url, "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 414
    }

    "return a 400 error on invalid URI" in withServer() { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft
    }

    "still serve requests if query string won't parse" in withServer() { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("POST", "/action?foo=query=bar=", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 200
      response.body must beLeft("_")
    }

    "allow accessing the raw unparsed path from an error handler" in withServer(new HttpErrorHandler() {
      def onClientError(request: RequestHeader, statusCode: Int, message: String) =
        Future.successful(Results.BadRequest("Bad path: " + request.path + " message: " + message))
      def onServerError(request: RequestHeader, exception: Throwable) = Future.successful(Results.Ok)
    }) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft("Bad path: /[ message: Cannot parse path from URI: /[")
    }
  }
}
