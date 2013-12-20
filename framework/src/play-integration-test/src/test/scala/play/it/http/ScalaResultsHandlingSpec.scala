package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.libs.ws.Response
import play.api.libs.iteratee._

import play.api.libs.concurrent.Execution.{defaultContext => ec}

object ScalaResultsHandlingSpec extends PlaySpecification {

  "scala body handling" should {

    def makeRequest[T](result: SimpleResult)(block: Response => T) = withServer(result) { implicit port =>
      val response = await(wsUrl("/").get())
      block(response)
    }

    def withServer[T](result: SimpleResult)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => Action(result)
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

    "close the connection for feed results" in withServer(
      Results.Ok.feed(Enumerator("a", "b", "c"))
    ) { port =>
      val response = BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )(0)
      response.status must_== 200
      response.headers.get(TRANSFER_ENCODING) must beNone
      response.headers.get(CONTENT_LENGTH) must beNone
      response.headers.get(CONNECTION) must beSome("close")
      response.body must beLeft("abc")
    }

    "close the HTTP 1.1 connection when requested" in withServer(
      Results.Ok.copy(connection = HttpConnection.Close)
    ) { port =>
      val response = BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )(0)
      response.status must_== 200
      response.headers.get(CONNECTION) must beSome("close")
    }

    "close the HTTP 1.0 connection when requested" in withServer(
      Results.Ok.copy(connection = HttpConnection.Close)
    ) { port =>
      val response = BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.0", Map("Connection" -> "keep-alive"), "")
      )(0)
      response.status must_== 200
      response.headers.get(CONNECTION) must beNone
    }

    "close the connection when the connection close header is present" in withServer(
      Results.Ok
    ) { port =>
      BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("Connection" -> "close"), "")
      )(0).status must_== 200
    }

    "close the connection when the connection when protocol is HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
      BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )(0).status must_== 200
    }

    "honour the keep alive header for HTTP 1.0" in withServer(
      Results.Ok
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.0", Map("Connection" -> "keep-alive"), ""),
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )
      responses(0).status must_== 200
      responses(0).headers.get(CONNECTION) must beSome("keep-alive")
      responses(1).status must_== 200
    }

    "keep alive HTTP 1.1 connections" in withServer(
      Results.Ok
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "close chunked connections when requested" in withServer(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { port =>
      // will timeout if not closed
      BasicHttpClient.makeRequests(port, checkClosed = true)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("Connection" -> "close"), "")
      )(0).status must_== 200
    }

    "keep chunked connections alive by default" in withServer(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "allow sending trailers" in withServer(
      SimpleResult(ResponseHeader(200, Map(TRANSFER_ENCODING -> CHUNKED, TRAILER -> "Chunks")),
        Enumerator("aa", "bb", "cc") &> Enumeratee.map[String](_.getBytes)(ec) &> Results.chunk(Some(
        Iteratee.fold[Array[Byte], Int](0)((count, in) => count + 1)(ec)
          .map(count => Seq("Chunks" -> count.toString))(ec)
      )))
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )(0)

      response.status must_== 200
      response.body must beRight
      val (chunks, trailers) = response.body.right.get
      chunks must containAllOf(Seq("aa", "bb", "cc")).inOrder
      trailers.get("Chunks") must beSome("3")
    }

    "fall back to simple streaming when more than one chunk is sent and protocol is HTTP 1.0" in withServer(
      SimpleResult(ResponseHeader(200, Map()), Enumerator("abc", "def", "ghi") &> Enumeratee.map[String](_.getBytes)(ec))
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )(0)
      response.headers.keySet must not contain TRANSFER_ENCODING
      response.headers.keySet must not contain CONTENT_LENGTH
      response.body must beLeft("abcdefghi")
    }

    "Strip malformed cookies" in withServer(
      Results.Ok
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("Cookie" -> """£"""), "")
      )(0)

      response.status must_== 200
      response.body must beLeft
    }

    "reject HTTP 1.0 requests for chunked results" in withServer(
      Results.Ok.chunked(Enumerator("a", "b", "c"))
    ) { port =>
      val response = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
      )(0)
      response.status must_== HTTP_VERSION_NOT_SUPPORTED
      response.body must beLeft("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
    }

    "not send empty chunks before the end of the enumerator stream" in makeRequest(
      Results.Ok.chunked(Enumerator("foo", "", "bar"))
    ) { response =>
      response.header(TRANSFER_ENCODING) must beSome("chunked")
      response.header(CONTENT_LENGTH) must beNone
      response.body must_== "foobar"
    }

  }

}
