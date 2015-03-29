package play.core.server.akkahttp

import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.test._
import play.core.server.ServerProvider
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

object AkkaHttpServerSpec extends PlaySpecification with WsTestClient {
  // Disable Akka HTTP tests by default until issues in Continuous Integration are resolved
  private val runTests: Boolean = (System.getProperty("run.akka.http.tests", "false") == "true")
  skipAllIf(!runTests)

  sequential

  def requestFromServer[T](
    path: String)(
      exec: WSRequest => Future[WSResponse])(
        routes: PartialFunction[(String, String), Handler])(
          check: WSResponse => T)(
            implicit awaitTimeout: Timeout): T = {
    running(TestServer(testServerPort, FakeApplication(withRoutes = routes), serverProvider = Some(AkkaHttpServer.defaultServerProvider))) {
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
      app = FakeApplication(withRoutes = httpServerTagRoutes),
      serverProvider = Some(AkkaHttpServer.defaultServerProvider)) {
      val response = await(wsUrl("/httpServerTag").get())
      response.status must equalTo(OK)
      response.body must_== "Some(akka-http)"
    }

    "start and stop cleanly" in {
      PlayRunners.mutex.synchronized {
        def testStartAndStop(i: Int) = {
          val resultString = s"result-$i"
          val app = FakeApplication(withRoutes = {
            case ("GET", "/") => Action(Ok(resultString))
          })
          val server = TestServer(testServerPort, app, serverProvider = Some(AkkaHttpServer.defaultServerProvider))
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
