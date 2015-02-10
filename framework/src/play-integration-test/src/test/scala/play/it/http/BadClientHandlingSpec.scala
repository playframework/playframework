/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.api._
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.it._
import scala.concurrent.Future
import scala.util.Random

object NettyBadClientHandlingSpec extends BadClientHandlingSpec with NettyIntegrationSpecification
object AkkaHttpBadClientHandlingSpec extends BadClientHandlingSpec with AkkaHttpIntegrationSpecification

trait BadClientHandlingSpec extends PlaySpecification with ServerIntegrationSpecification {

  "Play" should {

    def withServer[T](errorHandler: HttpErrorHandler = DefaultHttpErrorHandler)(block: Port => T) = {
      val port = testServerPort

      val app = new BuiltInComponentsFromContext(ApplicationLoader.createContext(Environment.simple())) {
        def router = Router.from {
          case _ => Action(Results.Ok)
        }
        override lazy val httpErrorHandler = errorHandler
      }.application

      running(TestServer(port, app)) {
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

    "allow accessing the raw unparsed path from an error handler" in withServer(new HttpErrorHandler() {
      def onClientError(request: RequestHeader, statusCode: Int, message: String) =
        Future.successful(Results.BadRequest("Bad path: " + request.path))
      def onServerError(request: RequestHeader, exception: Throwable) = Future.successful(Results.Ok)
    }) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft("Bad path: /[")
    }.skipUntilAkkaHttpFixed

  }
}
