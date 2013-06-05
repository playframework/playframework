package play.it.http

import org.specs2.mutable._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.Response
import play.api.libs.iteratee._
import java.net.Socket
import java.io.OutputStreamWriter
import org.apache.commons.io.IOUtils

import play.api.libs.concurrent.Execution.{defaultContext => ec}

object ScalaResultsHandlingSpec extends Specification {


  "scala body handling" should {

    def makeRequest[T](result: SimpleResult)(block: Response => T) = withServer(result) { implicit port =>
      val response = await(wsUrl("/").get())
      block(response)
    }

    // Low level stuff, for when our WS API isn't enough
    def makeBasicRequest(port: Int, lines: String*): Seq[String] = {
      val s = new Socket("localhost", port)
      try {
        s.setSoTimeout(5000)
        // Send request
        val out = new OutputStreamWriter(s.getOutputStream)
        var expectedResponses = 1
        lines.foreach { line =>
          out.write(line)
          out.write("\r\n")
        }
        out.write("\r\n")
        out.flush()

        import scala.collection.JavaConverters._
        IOUtils.readLines(s.getInputStream).asScala

      } finally {
        s.close()
      }
    }

    def withServer[T](result: SimpleResult)(block: Port => T) = withServerAction(Action(result))(block)

    def withServerAction[T](action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        }
      ))) {
        block(port)
      }
    }

    "buffer results with no content length" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "revert to chunked encoding when enumerator contains more than one item" in makeRequest(
      SimpleResult(ResponseHeader(200, Map()), Enumerator("abc", "def", "ghi") &> Enumeratee.map[String](_.getBytes)(ec))
    ) { response =>
        response.header(CONTENT_LENGTH) must beNone
        response.header(TRANSFER_ENCODING) must beSome("chunked")
        response.body must_== "abcdefghi"
      }

    "send results with a content length as is" in makeRequest(Results.Ok("Hello world")
      .withHeaders(CONTENT_LENGTH -> "5")) { response =>
      response.header(CONTENT_LENGTH) must beSome("5")
      response.body must_== "Hello"
    }

    "chunk results for chunked streaming strategy" in makeRequest(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    "close the connection for feed results" in makeRequest(
      Results.Ok.feed(Enumerator("a", "b", "c"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beNone
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "abc"
    }

    "close the connection when the connection close header is present" in withServer(
      Results.Ok
    ) { port =>
      // Will only return if the connection is closed by the server
      makeBasicRequest(port,
        "GET / HTTP/1.1",
        "Host: localhost",
        "Connection: close"
      )(0) must_== "HTTP/1.1 200 OK"
    }

    "close the connection when the connection when protocol is HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
    // Will only return if the connection is closed by the server
      makeBasicRequest(port,
        "GET / HTTP/1.0",
        "Host: localhost"
      )(0) must_== "HTTP/1.0 200 OK"
    }

    "honour the keep alive header for HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
      val lines = makeBasicRequest(port,
        "GET / HTTP/1.0",
        "Host: localhost",
        "Connection: keep-alive",
        "",
        "GET / HTTP/1.0",
        "Host: localhost"
      )
      // First response
      lines(0) must_== "HTTP/1.0 200 OK"
      // Second response will only exist if keep alive was honoured
      lines.tail must containAllOf(Seq("HTTP/1.0 200 OK"))
    }

    "keep alive HTTP 1.1 connections" in withServer(
      Results.Ok
    ) { port =>
      val lines = makeBasicRequest(port,
        "GET / HTTP/1.1",
        "Host: localhost",
        "",
        "GET / HTTP/1.1",
        "Host: localhost",
        "Connection: close"
      )
      // First response
      lines(0) must_== "HTTP/1.1 200 OK"
      // Second response will only exist if keep alive was honoured
      lines.tail must containAllOf(Seq("HTTP/1.1 200 OK"))
    }

    "close chunked connections when requested" in withServer(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { port =>
      // will timeout if not closed
      makeBasicRequest(port,
        "GET / HTTP/1.1",
        "Host: localhost",
        "Connection: close"
      )(0) must_== "HTTP/1.1 200 OK"
    }

    "keep chunked connections alive by default" in withServer(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { port =>
      val lines = makeBasicRequest(port,
        "GET / HTTP/1.1",
        "Host: localhost",
        "",
        "GET / HTTP/1.1",
        "Host: localhost",
        "Connection: close"
      )
      // First response
      lines(0) must_== "HTTP/1.1 200 OK"
      // Second response will only exist if keep alive was honoured
      lines.tail must containAllOf(Seq("HTTP/1.1 200 OK"))
        .orSkip("There is a race condition between the socket closing and the responses received")
    }

    "allow sending trailers" in withServer(
      SimpleResult(ResponseHeader(200, Map(TRANSFER_ENCODING -> CHUNKED, TRAILER -> "Chunks")),
        Enumerator("aa", "bb", "cc") &> Enumeratee.map[String](_.getBytes)(ec) &> Results.chunk(Some(
        Iteratee.fold[Array[Byte], Int](0)((count, in) => count + 1)(ec)
          .map(count => Seq("Chunks" -> count.toString))(ec)
      )))
    ) { port =>
      val lines = makeBasicRequest(port,
        "GET / HTTP/1.1",
        "Host: localhost",
        "Connection: close"
      )
      // Assert each chunk is there
      lines must containAllOf(Seq("aa", "bb", "cc"))
      // Assertion on last chunk
      lines(lines.length - 3) must_== "0"
      lines(lines.length - 2) must_== "Chunks: 3"
      lines(lines.length - 1) must_== ""
    }

    "fall back to simple streaming when more than one chunk is sent and protocol is HTTP 1.0" in withServer(
      SimpleResult(ResponseHeader(200, Map()), Enumerator("abc", "def", "ghi") &> Enumeratee.map[String](_.getBytes)(ec))
    ) { port =>
      val lines = makeBasicRequest(port,
        "GET / HTTP/1.0",
        "Host: localhost"
      )
      lines.foreach { _ must not contain "Transfer-Encoding" }
      lines.last must_== "abcdefghi"
    }

    "honour 100 continue" in withServer(
      Results.Ok
    ) { port =>
      val lines = makeBasicRequest(port,
        "POST / HTTP/1.1",
        "Host: localhost",
        "Expect: 100-continue",
        "Connection: close"
      )
      lines(0) must_== "HTTP/1.1 100 Continue"
      lines must containAllOf(Seq("HTTP/1.1 200 OK"))
    }

    "not read body when expecting 100 continue but action iteratee is done" in withServerAction(new EssentialAction {
      def apply(v1: RequestHeader) = Done(Results.Ok)
    }) { port =>
      val lines = makeBasicRequest(port,
        "POST / HTTP/1.1",
        "Host: localhost",
        "Expect: 100-continue",
        "Connection: close",
        "Content-Length: 10000000"
      )
      lines(0) must_== ("HTTP/1.1 200 OK")
    }

  }

}
