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
        response.setHeader("Server", "foo")
        response.setHeader("server", "bar")
        Results.ok("Hello world").withHeader("Other", "foo").withHeader("other", "bar")
      }
    }) { response =>
      response.header("Server") must beSome("bar")
      response.header("Other") must beSome("bar")
      response.body must_== "Hello world"
    }

    "send strict results" in makeRequest(new MockController {
      def action = Results.ok("Hello world")
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
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
        case value => value.toLowerCase(java.util.Locale.ENGLISH) must_== "text/event-stream; charset=utf-8"
      }
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "data: a\n\ndata: b\n\n"
    }

    "stream input stream responses as chunked" in makeRequest(new MockController {
      def action = {
        Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")))
      }
    }) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.body must_== "hello"
    }

    "not chunk input stream results if a content length is set" in makeRequest(new MockController {
      def action = {
        // chunk size 2 to force more than one chunk
        Results.ok(new ByteArrayInputStream("hello".getBytes("utf-8")), 5)
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.header(TRANSFER_ENCODING) must beNone
      response.body must_== "hello"
    }

  }
}
