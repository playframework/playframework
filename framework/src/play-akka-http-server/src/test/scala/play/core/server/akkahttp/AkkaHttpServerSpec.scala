/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.test._
import scala.concurrent.Future
import akka.util.Timeout

object AkkaHttpServerSpec extends PlaySpecification with WsTestClient {
  // Provide a flag to disable Akka HTTP tests
  private val runTests: Boolean = (System.getProperty("run.akka.http.tests", "true") == "true")
  skipAllIf(!runTests)

  sequential

  def requestFromServer[T](path: String)(exec: WSRequest => Future[WSResponse])(routes: PartialFunction[(String, String), Handler])(check: WSResponse => T)(implicit awaitTimeout: Timeout): T = {
    val app = GuiceApplicationBuilder().routes(routes).build()
    running(TestServer(testServerPort, app, serverProvider = Some(AkkaHttpServer.provider))) {
      val plainRequest = wsUrl(path)(testServerPort)
      val responseFuture = exec(plainRequest)
      val response = await(responseFuture)(awaitTimeout)
      check(response)
    }
  }

  "AkkaHttpServer" should {

    "send hello world" in {
      // This test experiences CI timeouts. Give it more time.
      val reallyLongTimeout = Timeout(defaultAwaitTimeout.duration * 3)
      requestFromServer("/hello") { request =>
        request.get()
      } {
        case ("GET", "/hello") => Action(Ok("greetings"))
      } { response =>
        response.body must_== "greetings"
      }(reallyLongTimeout)
    }

    "send responses when missing a Content-Length" in {
      requestFromServer("/hello") { request =>
        request.get()
      } {
        case ("GET", "/hello") => Action(Ok("greetings"))
      } { response =>
        response.status must_== 200
        response.header(CONTENT_TYPE) must_== Some("text/plain; charset=UTF-8")
        response.header(CONTENT_LENGTH) must_== Some("9")
        response.header(TRANSFER_ENCODING) must_== None
        response.body must_== "greetings"
      }
    }

    "not send chunked responses when given a Content-Length" in {
      requestFromServer("/hello") { request =>
        request.get()
      } {
        case ("GET", "/hello") => Action {
          Ok("greetings").withHeaders(CONTENT_LENGTH -> "9")
        }
      } { response =>
        response.status must_== 200
        response.header(CONTENT_TYPE) must_== Some("text/plain; charset=UTF-8")
        response.header(CONTENT_LENGTH) must_== Some("9")
        response.header(TRANSFER_ENCODING) must_== None
        response.body must_== "greetings"
      }
    }

    def headerDump(headerNames: String*)(implicit request: Request[_]): String = {
      val headerGroups: Seq[String] = for (n <- headerNames) yield {
        val headerGroup = request.headers.getAll(n)
        headerGroup.mkString("<", ", ", ">")
      }
      headerGroups.mkString("; ")
    }

    "pass request headers to Actions" in {
      requestFromServer("/abc") { request =>
        request.withHeaders(
          ACCEPT_ENCODING -> "utf-8",
          ACCEPT_LANGUAGE -> "en-NZ").get()
      } {
        case ("GET", "/abc") => Action { implicit request =>
          Ok(headerDump(ACCEPT_ENCODING, ACCEPT_LANGUAGE))
        }
      } { response =>
        response.status must_== 200
        response.body must_== "<utf-8>; <en-NZ>"
      }
    }

    "pass raw request URI to Actions" in {
      requestFromServer("/abc?foo=bar") { request =>
        request.withHeaders(
          ACCEPT_ENCODING -> "utf-8",
          ACCEPT_LANGUAGE -> "en-US"
        ).get()
      } {
        case ("GET", "/abc") => Action { implicit request =>
          Ok(request.uri)
        }
      } { response =>
        response.status must_== 200
        response.body must_== "/abc?foo=bar"
      }
    }

    "pass raw request uri to Actions even if Raw-Request-URI header is set" in {
      import akka.http.scaladsl.model.headers._
      requestFromServer("/abc?foo=bar") { request =>
        request.withHeaders(
          ACCEPT_ENCODING -> "utf-8",
          ACCEPT_LANGUAGE -> "en-US",
          `Raw-Request-URI`.name -> "/foo/bar/baz"
        ).get()
      } {
        case ("GET", "/abc") => Action { implicit request =>
          Ok(request.uri)
        }
      } { response =>
        response.status must_== 200
        response.body must_== "/abc?foo=bar"
      }
    }

    "pass POST request bodies to Actions" in {
      requestFromServer("/greet") { request =>
        request.post("Bob")
      } {
        case ("POST", "/greet") => Action(parse.text) { implicit request =>
          val name = request.body
          Ok(s"Hello $name")
        }
      } { response =>
        response.status must_== 200
        response.body must_== "Hello Bob"
      }
    }

    "send response statüs" in {
      requestFromServer("/def") { request =>
        request.get()
      } {
        case ("GET", "/abc") => Action { implicit request =>
          ???
        }
      } { response =>
        response.status must_== 404
      }
    }

    val httpServerTagRoutes: PartialFunction[(String, String), Handler] = {
      case ("GET", "/httpServerTag") => Action { implicit request =>
        val httpServer = request.tags.get("HTTP_SERVER")
        Ok(httpServer.toString)
      }
    }

    "pass tag of HTTP_SERVER->akka-http to Actions" in {
      requestFromServer("/httpServerTag") { request =>
        request.get()
      } {
        case ("GET", "/httpServerTag") => Action { implicit request =>
          val httpServer = request.tags.get("HTTP_SERVER")
          Ok(httpServer.toString)
        }
      } { response =>
        response.status must_== 200
        response.body must_== "Some(akka-http)"
      }
    }

    "support WithServer form" in new WithServer(
      app = GuiceApplicationBuilder().routes(httpServerTagRoutes).build(),
      serverProvider = Some(AkkaHttpServer.provider)) {
      val response = await(wsUrl("/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== "Some(akka-http)"
    }

    "start and stop cleanly" in {
      PlayRunners.mutex.synchronized {
        def testStartAndStop(i: Int) = {
          val resultString = s"result-$i"
          val app = GuiceApplicationBuilder().routes {
            case ("GET", "/") => Action(Ok(resultString))
          }.build()
          val server = TestServer(testServerPort, app, serverProvider = Some(AkkaHttpServer.provider))
          server.start()
          try {
            val response = await(wsUrl("/")(testServerPort).get())
            response.body must_== resultString
          } finally {
            server.stop()
          }
        }
        // Start and stop the server 20 times
        (0 until 20) must contain { (i: Int) => testStartAndStop(i) }
      }
    }

  }
}
