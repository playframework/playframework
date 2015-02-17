/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import java.io.{ ByteArrayInputStream, IOException }
import play.api.Application
import play.api.test._
import play.api.libs.ws.WSResponse
import play.it._
import play.libs.EventSource
import play.libs.EventSource.Event
import play.mvc.Results
import play.mvc.Results.Chunks
import scala.util.{ Failure, Success, Try }

object NettyJavaResultsHandlingSpec extends JavaResultsHandlingSpec with NettyIntegrationSpecification
object AkkaHttpJavaResultsHandlingSpec extends JavaResultsHandlingSpec with AkkaHttpIntegrationSpecification

trait JavaResultsHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  "Java results handling" should {
    def makeRequest[T](controller: MockController)(block: WSResponse => T) = {
      implicit val port = testServerPort
      lazy val app: Application = FakeApplication(
        withRoutes = {
          case _ => JAction(app, controller)
        }
      )

      running(TestServer(port, app)) {
        val response = await(wsUrl("/").get())
        block(response)
      }
    }

    "treat headers case insensitively" in makeRequest(new MockController {
      def action = {
        response.setHeader("content-type", "text/plain")
        response.setHeader("Content-type", "text/html")
        Results.ok("Hello world")
      }
    }) { response =>
      response.header(CONTENT_TYPE) must beSome("text/html")
      response.body must_== "Hello world"
    }

    "buffer results with no content length" in makeRequest(new MockController {
      def action = Results.ok("Hello world")
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "truncate results or close connection if content length is too short" in {
      val controller = new MockController {
        def action = {
          response.setHeader(CONTENT_LENGTH, "5")
          Results.ok("Hello world")
        }
      }
      implicit val port = testServerPort
      lazy val app: Application = FakeApplication(
        withRoutes = {
          case _ => JAction(app, controller)
        }
      )
      // We accept different behaviors for different HTTP
      // backends. Either behavior is OK.
      running(TestServer(port, app)) {
        Try(await(wsUrl("/").get())) match {
          case Success(response) =>
            response.header(CONTENT_LENGTH) must beSome("5")
            response.body must_== "Hello" // Truncated
          case Failure(t) =>
            t must haveClass[IOException] // Connection closed
        }
      }
    }

    "chunk results that are streamed" in makeRequest(new MockController {
      def action = {
        Results.ok(new Results.StringChunks() {
          def onReady(out: Chunks.Out[String]) {
            out.write("a")
            out.write("b")
            out.write("c")
            out.close()
          }
        })
      }
    }) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    "chunk event source results" in makeRequest(new MockController {
      def action = {
        Results.ok(new EventSource() {
          def onConnected(): Unit = {
            send(Event.event("a"))
            send(Event.event("b"))
            close()
          }
        })
      }
    }) { response =>
      response.header(CONTENT_TYPE) must beSome.like {
        case value => value.toLowerCase must_== "text/event-stream; charset=utf-8"
      }
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "data: a\n\ndata: b\n\n"
    }

    "buffer input stream results of one chunk" in makeRequest(new MockController {
      def action = {
        Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")))
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.header(TRANSFER_ENCODING) must beNone
      response.body must_== "hello"
    }

    "chunk input stream results of more than one chunk" in makeRequest(new MockController {
      def action = {
        // chunk size 2 to force more than one chunk
        Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")), 2)
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beNone
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.body must_== "hello"
    }

    "not chunk input stream results if a content length is set" in makeRequest(new MockController {
      def action = {
        response.setHeader(CONTENT_LENGTH, "5")
        // chunk size 2 to force more than one chunk
        Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")), 2)
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.header(TRANSFER_ENCODING) must beNone
      response.body must_== "hello"
    }

    "not chunk input stream results if HTTP/1.0 is in use" in {
      implicit val port = testServerPort

      lazy val app: Application = FakeApplication(
        withRoutes = {
          case _ => JAction(app, new MockController {
            def action = {
              // chunk size 2 to force more than one chunk
              Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")), 2)
            }
          })
        }
      )

      running(TestServer(port, app)) {
        val response = BasicHttpClient.makeRequests(testServerPort, true)(
          BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
        )(0)
        response.headers.get(CONTENT_LENGTH) must beNone
        response.headers.get(TRANSFER_ENCODING) must beNone
        response.body must beLeft("hello")
      }

    }
  }
}
