/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import java.io.ByteArrayInputStream
import java.util.Arrays

import akka.NotUsed
import akka.stream.javadsl.Source
import com.fasterxml.jackson.databind.JsonNode
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.libs.ws.WSResponse
import play.it._
import play.libs.{ Comet, EventSource, Json, LegacyEventSource }
import play.mvc.Http.MimeTypes
import play.mvc.Results
import play.mvc.Results.Chunks

object NettyJavaResultsHandlingSpec extends JavaResultsHandlingSpec with NettyIntegrationSpecification
object AkkaHttpJavaResultsHandlingSpec extends JavaResultsHandlingSpec with AkkaHttpIntegrationSpecification

trait JavaResultsHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  "Java results handling" should {
    def makeRequest[T](controller: MockController)(block: WSResponse => T) = {
      implicit val port = testServerPort
      lazy val app: Application = GuiceApplicationBuilder().routes {
        case _ => JAction(app, controller)
      }.build()

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

    "chunk legacy event source results" in makeRequest(new MockController {
      def action = {
        Results.ok(new LegacyEventSource() {
          def onConnected(): Unit = {
            send(LegacyEventSource.Event.event("a"))
            send(LegacyEventSource.Event.event("b"))
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

    "chunk comet results from string" in makeRequest(new MockController {
      def action = {
        import scala.collection.JavaConverters._
        val dataSource = akka.stream.javadsl.Source.from(List("a", "b", "c").asJava)
        val cometSource = dataSource.via(Comet.string("callback"))
        Results.ok().chunked(cometSource)
      }
    }) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must contain("<html><body><script type=\"text/javascript\">callback('a');</script><script type=\"text/javascript\">callback('b');</script><script type=\"text/javascript\">callback('c');</script>")
    }

    "chunk comet results from json" in makeRequest(new MockController {
      def action = {
        val objectNode = Json.newObject
        objectNode.put("foo", "bar")
        val dataSource: Source[JsonNode, NotUsed] = akka.stream.javadsl.Source.from(Arrays.asList(objectNode))
        val cometSource = dataSource.via(Comet.json("callback"))
        Results.ok().chunked(cometSource)
      }
    }) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must contain("<html><body><script type=\"text/javascript\">callback({\"foo\":\"bar\"});</script>")
    }

    "chunk event source results" in makeRequest(new MockController {
      def action = {
        import scala.collection.JavaConverters._
        val dataSource = akka.stream.javadsl.Source.from(List("a", "b").asJava).map {
          new akka.japi.function.Function[String, EventSource.Event] {
            def apply(t: String) = EventSource.Event.event(t)
          }
        }
        val eventSource = dataSource.via(EventSource.flow())
        Results.ok().chunked(eventSource).as("text/event-stream")
      }
    }) { response =>
      response.header(CONTENT_TYPE) must beSome.like {
        case value => value.toLowerCase(java.util.Locale.ENGLISH) must_== "text/event-stream"
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
