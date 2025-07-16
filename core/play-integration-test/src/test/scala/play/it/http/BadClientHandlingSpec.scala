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

class NettyBadClientHandlingSpec     extends BadClientHandlingSpec with NettyIntegrationSpecification
class PekkoHttpBadClientHandlingSpec extends BadClientHandlingSpec with PekkoHttpIntegrationSpecification

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

    "allow accessing the raw unparsed path and request-id from an error handler" in withServer(new HttpErrorHandler() {
      def onClientError(request: RequestHeader, statusCode: Int, message: String) =
        Future.successful(
          Results.BadRequest("Bad path: " + request.path + " message: " + message + " r.id: " + request.id)
        )
      def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
    }) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      val expectedBodyTrailing = "Bad path: /[ message: Cannot parse path from URI: /[ r.id: "
      response.status must_== 400
      response.body.isLeft must_== true
      val responseBody = response.body.swap.getOrElse("<empty>")
      responseBody must startWith(expectedBodyTrailing)
      responseBody.substring(expectedBodyTrailing.length).matches("[0-9]+") must_== true // must have request id
    }

    "allow accessing (empty) cookies, (empty) session and (empty) flash from an error handler if no headers are given" in withServer(
      new HttpErrorHandler() {
        def onClientError(request: RequestHeader, statusCode: Int, message: String) =
          Future.successful(
            Results.BadRequest(
              "cookies: " + request.cookies + " session: " + request.session + " flash: " + request.flash
            )
          )
        def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
      }
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/[", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 400
      response.body must beLeft("cookies: Iterable() session: Session(Map()) flash: Flash(Map())")
    }

    "allow accessing cookies, session and flash from an error handler if headers are set" in withServer(
      new HttpErrorHandler() {
        def onClientError(request: RequestHeader, statusCode: Int, message: String) =
          Future.successful(
            Results.BadRequest(
              "cookies: " + request.cookies + " session: " + request.session + " flash: " + request.flash
            )
          )
        def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(Results.Ok)
      }
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest(
          "GET",
          "/[",
          "HTTP/1.1",
          Map(
            "Cookie" ->
              ("PLAY_SESSION=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InNlc3Npb25mb28iOiJzZXNzaW9uYmFyIn0sIm5iZiI6MTc1MjY2ODA1NSwiaWF0IjoxNzUyNjY4MDU1fQ.HgN1CB4OqFE7NlAwuOKMpn5733_wXq295wC_gX34VvU; " +
                "PLAY_FLASH=eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImZsYXNoZm9vIjoiZmxhc2hiYXIifSwibmJmIjoxNzUyNjY3OTg0LCJpYXQiOjE3NTI2Njc5ODR9.LXzAn-N8BnlodhFhG3Q4YGAVd47jqq7gGAGrYCrLCEQ")
          ),
          ""
        )
      )(0)

      response.status must_== 400
      response.body must beLeft(
        "cookies: Map(PLAY_SESSION -> Cookie(PLAY_SESSION,eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InNlc3Npb25mb28iOiJzZXNzaW9uYmFyIn0sIm5iZiI6MTc1MjY2ODA1NSwiaWF0IjoxNzUyNjY4MDU1fQ.HgN1CB4OqFE7NlAwuOKMpn5733_wXq295wC_gX34VvU,None,/,None,false,true,None,false), PLAY_FLASH -> Cookie(PLAY_FLASH,eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImZsYXNoZm9vIjoiZmxhc2hiYXIifSwibmJmIjoxNzUyNjY3OTg0LCJpYXQiOjE3NTI2Njc5ODR9.LXzAn-N8BnlodhFhG3Q4YGAVd47jqq7gGAGrYCrLCEQ,None,/,None,false,true,None,false)) " +
          "session: Session(Map(sessionfoo -> sessionbar)) " +
          "flash: Flash(Map(flashfoo -> flashbar))"
      )
    }
  }
}
