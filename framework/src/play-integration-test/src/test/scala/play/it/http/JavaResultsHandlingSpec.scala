/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.ByteArrayInputStream
import java.util.{ Arrays, Optional }

import akka.NotUsed
import akka.stream.javadsl.Source
import akka.util.ByteString
import com.fasterxml.jackson.databind.JsonNode
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.libs.ws.WSResponse
import play.http.HttpEntity
import play.it._
import play.libs.{ Comet, EventSource, Json }
import play.mvc.Http.{ Cookie, Flash, Session }
import play.mvc._

class NettyJavaResultsHandlingSpec extends JavaResultsHandlingSpec with NettyIntegrationSpecification
class AkkaHttpJavaResultsHandlingSpec extends JavaResultsHandlingSpec with AkkaHttpIntegrationSpecification

trait JavaResultsHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  "Java results handling" should {
    def makeRequest[T](controller: MockController, additionalConfig: Map[String, String] = Map.empty, followRedirects: Boolean = true)(block: WSResponse => T) = {
      implicit val port = testServerPort
      lazy val app: Application = GuiceApplicationBuilder().configure(additionalConfig).routes {
        case _ => JAction(app, controller)
      }.build()

      running(TestServer(port, app)) {
        val response = await(wsUrl("/").withFollowRedirects(followRedirects).get())
        block(response)
      }
    }

    "add Date header" in makeRequest(new MockController {
      def action = {
        Results.ok("Hello world")
      }
    }) { response =>
      response.header(DATE) must beSome
    }

    "work with non-standard HTTP response codes" in makeRequest(new MockController {
      def action = {
        Results.status(498)
      }
    }) { response =>
      response.status must beEqualTo(498)
    }

    "add Content-Length for strict results" in makeRequest(new MockController {
      def action = {
        Results.ok("Hello world")
      }
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "support responses with custom Content-Types" in makeRequest(new MockController {
      def action = {
        val entity = new HttpEntity.Strict(ByteString(0xff.toByte), Optional.of("schmitch/foo; bar=bax"))
        new StatusHeader(OK).sendEntity(entity)
      }
    }) { response =>
      response.header(CONTENT_TYPE) must beSome("schmitch/foo; bar=bax")
      response.header(CONTENT_LENGTH) must beSome("1")
      response.header(TRANSFER_ENCODING) must beNone
      response.bodyAsBytes must_== ByteString(0xff.toByte)
    }

    "support multipart/mixed responses" in {
      val contentType = """multipart/mixed; boundary="simple boundary""""
      val body: String =
        """|This is the preamble.  It is to be ignored, though it
           |is a handy place for mail composers to include an
           |explanatory note to non-MIME compliant readers.
           |--simple boundary
           |
           |This is implicitly typed plain ASCII text.
           |It does NOT end with a linebreak.
           |--simple boundary
           |Content-type: text/plain; charset=us-ascii
           |
           |This is explicitly typed plain ASCII text.
           |It DOES end with a linebreak.
           |
           |--simple boundary--
           |This is the epilogue.  It is also to be ignored.""".stripMargin

      makeRequest(new MockController {
        def action = {
          val entity = new HttpEntity.Strict(ByteString(body), Optional.of(contentType))
          new StatusHeader(OK).sendEntity(entity)
        }
      }) { response =>
        response.header(CONTENT_TYPE) must beSome(contentType)
        response.header(CONTENT_LENGTH) must beSome(body.length.toString)
        response.header(TRANSFER_ENCODING) must beNone
        response.body must_== body
      }
    }

    "serve a JSON with UTF-8 charset" in makeRequest(new MockController {
      def action = {
        val objectNode = Json.newObject
        objectNode.put("foo", "bar")
        Results.ok(objectNode)
      }
    }) { response =>
      response.header(CONTENT_TYPE) must (
        // There are many valid responses, but for simplicity just hardcode the two responses that
        // the Netty and Akka HTTP backends actually return.
        beSome("application/json; charset=UTF-8") or
        beSome("application/json")
      )
    }

    "serve a XML with correct Content-Type" in makeRequest(new MockController {
      def action = {
        Results.ok("<name>marcos</name>").as("application/xml;charset=Windows-1252")
      }
    }) { response =>
      response.header(CONTENT_TYPE) must (
        // There are many valid responses, but for simplicity just hardcode the two responses that
        // the Netty and Akka HTTP backends actually return.
        beSome("application/xml; charset=windows-1252") or beSome("application/xml;charset=Windows-1252")
      )
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

    "add cookies in Result" in makeRequest(new MockController {
      def action = {
        Results.ok("Hello world")
          .withCookies(new Http.Cookie("bar", "KitKat", 1000, "/", "example.com", false, true, null))
          .withCookies(new Http.Cookie("framework", "Play", 1000, "/", "example.com", false, true, null))
      }
    }) { response =>
      response.headers("Set-Cookie") must contain((s: String) => s.startsWith("bar=KitKat;"))
      response.headers("Set-Cookie") must contain((s: String) => s.startsWith("framework=Play;"))
      response.body must_== "Hello world"
    }

    "add cookies with SameSite policy in Result" in makeRequest(new MockController {
      def action = {
        Results.ok("Hello world")
          .withCookies(Http.Cookie.builder("bar", "KitKat").withSameSite(Http.Cookie.SameSite.LAX).build())
          .withCookies(Http.Cookie.builder("framework", "Play").withSameSite(Http.Cookie.SameSite.STRICT).build())
      }
    }) { response =>
      val cookieHeader: Seq[String] = response.headers("Set-Cookie")
      cookieHeader(0) must contain("bar=KitKat")
      cookieHeader(0) must contain("SameSite=Lax")

      cookieHeader(1) must contain("framework=Play")
      cookieHeader(1) must contain("SameSite=Strict")
    }

    "honor configuration for play.http.session.sameSite" in {
      "when configured to lax" in makeRequest(new MockController {
        def action = {
          import scala.collection.JavaConverters._

          val responseHeader = new ResponseHeader(OK, Map.empty[String, String].asJava)
          val body = HttpEntity.fromString("Hello World", "utf-8")
          val session = new Session(Map.empty[String, String].asJava)
          val flash = new Flash(Map.empty[String, String].asJava)
          val cookies = List.empty[Cookie].asJava

          val result = new Result(responseHeader, body, session, flash, cookies)
          result.session().put("bar", "KitKat")
          result
        }
      }, Map("play.http.session.sameSite" -> "lax")) { response =>
        response.header("Set-Cookie") must beSome.which(_.contains("SameSite=Lax"))
      }

      "when configured to strict" in makeRequest(new MockController {
        def action = {
          import scala.collection.JavaConverters._

          val responseHeader = new ResponseHeader(OK, Map.empty[String, String].asJava)
          val body = HttpEntity.fromString("Hello World", "utf-8")
          val session = new Session(Map.empty[String, String].asJava)
          val flash = new Flash(Map.empty[String, String].asJava)
          val cookies = List.empty[Cookie].asJava

          val result = new Result(responseHeader, body, session, flash, cookies)
          result.session().put("bar", "KitKat")
          result
        }
      }, Map("play.http.session.sameSite" -> "strict")) { response =>
        response.header("Set-Cookie") must beSome.which(_.contains("SameSite=Strict"))
      }
    }

    "handle duplicate withCookies in Result" in {
      val result = Results.ok("Hello world")
        .withCookies(new Http.Cookie("bar", "KitKat", 1000, "/", "example.com", false, true, null))
        .withCookies(new Http.Cookie("bar", "Mars", 1000, "/", "example.com", false, true, null))

      import scala.collection.JavaConverters._
      val cookies = result.cookies().iterator().asScala.toList
      val cookieValues = cookies.map(_.value)
      cookieValues must not contain ("KitKat")
      cookieValues must contain("Mars")
    }

    "handle duplicate cookies" in makeRequest(new MockController {
      def action = {
        Results.ok("Hello world")
          .withCookies(new Http.Cookie("bar", "KitKat", 1000, "/", "example.com", false, true, null))
          .withCookies(new Http.Cookie("bar", "Mars", 1000, "/", "example.com", false, true, null))
      }
    }) { response =>
      response.headers("Set-Cookie") must contain((s: String) => s.startsWith("bar=Mars;"))
      response.body must_== "Hello world"
    }

    "add cookies in Response" in makeRequest(new MockController {
      def action = {
        response.setCookie(new Http.Cookie("foo", "1", 1000, "/", "example.com", false, true, null))
        Results.ok("Hello world")
      }
    }) { response =>
      response.header("Set-Cookie").get must contain("foo=1;")
      response.body must_== "Hello world"
    }

    "add transient cookies in Response" in makeRequest(new MockController {
      def action = {
        response.setCookie(new Http.Cookie("foo", "1", null, "/", "example.com", false, true, null))
        Results.ok("Hello world")
      }
    }) { response =>
      response.header("Set-Cookie").get.toLowerCase must not contain "max-age="
      response.body must_== "Hello world"
    }

    "clear Session" in makeRequest(new MockController {
      def action = {
        session.clear()
        Results.ok("Hello world")
      }
    }) { response =>
      response.header("Set-Cookie").get must contain("PLAY_SESSION=; Max-Age=0")
      response.body must_== "Hello world"
    }

    "add cookies in both Response and Result" in makeRequest(new MockController {
      def action = {
        response.setCookie(new Http.Cookie("foo", "1", 1000, "/", "example.com", false, true, null))
        Results.ok("Hello world").withCookies(
          new Http.Cookie("bar", "KitKat", 1000, "/", "example.com", false, true, null)
        )
      }
    }) { response =>
      response.headers("Set-Cookie")(0) must contain("bar=KitKat")
      response.headers("Set-Cookie")(1) must contain("foo=1")
      response.body must_== "Hello world"
    }

    "send strict results" in makeRequest(new MockController {
      def action = Results.ok("Hello world")
    }) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
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
