/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import java.util.Locale.ENGLISH
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.libs.ws._
import play.api.libs.iteratee._
import play.api.libs.EventSource
import play.it._
import scala.util.Try
import play.api.http.{ HttpEntity, HttpChunk, Status }

object NettyScalaResultsHandlingSpec extends ScalaResultsHandlingSpec with NettyIntegrationSpecification
object AkkaHttpScalaResultsHandlingSpec extends ScalaResultsHandlingSpec with AkkaHttpIntegrationSpecification

trait ScalaResultsHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  "scala body handling" should {

    def tryRequest[T](result: Result)(block: Try[WSResponse] => T) = withServer(result) { implicit port =>
      val response = Try(await(wsUrl("/").get()))
      block(response)
    }

    def makeRequest[T](result: Result)(block: WSResponse => T) = {
      tryRequest(result)(tryResult => block(tryResult.get))
    }

    def withServer[T](result: Result)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, GuiceApplicationBuilder().routes { case _ => Action(result) }.build())) {
        block(port)
      }
    }

    "add Date header" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(DATE) must beSome
    }

    "add Content-Length for strict results" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    "not add a content length header when none is supplied" in makeRequest(
      Results.Ok.sendEntity(HttpEntity.Streamed(Source(List("abc", "def", "ghi")).map(ByteString.apply), None, None))
    ) { response =>
        response.header(CONTENT_LENGTH) must beNone
        response.header(TRANSFER_ENCODING) must beNone
        response.body must_== "abcdefghi"
      }

    "chunk results for chunked streaming strategy" in makeRequest(
      Results.Ok.chunked(Source(List("a", "b", "c")))
    ) { response =>
        response.header(TRANSFER_ENCODING) must beSome("chunked")
        response.header(CONTENT_LENGTH) must beNone
        response.body must_== "abc"
      }

    "chunk results for event source strategy" in makeRequest(
      Results.Ok.chunked(Source(List("a", "b")) via EventSource.flow).as("text/event-stream")
    ) { response =>
        response.header(CONTENT_TYPE) must beSome.like {
          case value => value.toLowerCase(java.util.Locale.ENGLISH) must_== "text/event-stream"
        }
        response.header(TRANSFER_ENCODING) must beSome("chunked")
        response.header(CONTENT_LENGTH) must beNone
        response.body must_== "data: a\n\ndata: b\n\n"
      }

    "close the connection when no content length is sent" in withServer(
      Results.Ok.sendEntity(HttpEntity.Streamed(Source.single(ByteString("abc")), None, None))
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
      Results.Ok.withHeaders(CONNECTION -> "close")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port, checkClosed = true)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )(0)
        response.status must_== 200
        response.headers.get(CONNECTION) must beSome("close")
      }

    "close the HTTP 1.0 connection when requested" in withServer(
      Results.Ok.withHeaders(CONNECTION -> "close")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port, checkClosed = true)(
          BasicRequest("GET", "/", "HTTP/1.0", Map("Connection" -> "keep-alive"), "")
        )(0)
        response.status must_== 200
        response.headers.get(CONNECTION).map(_.toLowerCase(ENGLISH)) must beOneOf(None, Some("close"))
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
        responses(0).headers.get(CONNECTION) must beSome.like {
          case s => s.toLowerCase(ENGLISH) must_== "keep-alive"
        }
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
      Results.Ok.chunked(Source(List("a", "b", "c")))
    ) { port =>
        // will timeout if not closed
        BasicHttpClient.makeRequests(port, checkClosed = true)(
          BasicRequest("GET", "/", "HTTP/1.1", Map("Connection" -> "close"), "")
        ).head.status must_== 200
      }

    "keep chunked connections alive by default" in withServer(
      Results.Ok.chunked(Source(List("a", "b", "c")))
    ) { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )
        responses(0).status must_== 200
        responses(1).status must_== 200
      }

    "allow sending trailers" in withServer(
      Result(ResponseHeader(200, Map(TRANSFER_ENCODING -> CHUNKED, TRAILER -> "Chunks")),
        HttpEntity.Chunked(Source(List(
          chunk("aa"), chunk("bb"), chunk("cc"), HttpChunk.LastChunk(new Headers(Seq("Chunks" -> "3")))
        )), None)
      )
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
      Results.Ok.chunked(Source(List("a", "b", "c")))
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
        )(0)
        response.status must_== HTTP_VERSION_NOT_SUPPORTED
        response.body must beLeft("The response to this request is chunked and hence requires HTTP 1.1 to be sent, but this is a HTTP 1.0 request.")
      }

    "return a 500 error on response with null header" in withServer(
      Results.Ok("some body").withHeaders("X-Null" -> null)
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        response.status must_== 500
        response.body must beLeft("")
      }

    "return a 400 error on Header value contains a prohibited character" in withServer(
      Results.Ok
    ) { port =>

        forall(List(
          "aaa" -> "bbb\fccc",
          "ddd" -> "eee\u000bfff"
        )) { header =>

          val response = BasicHttpClient.makeRequests(port)(
            BasicRequest("GET", "/", "HTTP/1.1", Map(header), "")
          ).head

          response.status must_== 400
          response.body must beLeft
        }
      }

    "split Set-Cookie headers" in {
      import play.api.mvc.Cookie
      val aCookie = Cookie("a", "1")
      val bCookie = Cookie("b", "2")
      val cCookie = Cookie("c", "3")
      makeRequest {
        Results.Ok.withCookies(aCookie, bCookie, cCookie)
      } { response =>
        response.allHeaders.get(SET_COOKIE) must beSome.like {
          case rawCookieHeaders =>
            val decodedCookieHeaders: Set[Set[Cookie]] = rawCookieHeaders.map { headerValue =>
              Cookies.decodeSetCookieHeader(headerValue).to[Set]
            }.to[Set]
            decodedCookieHeaders must_== (Set(Set(aCookie), Set(bCookie), Set(cCookie)))
        }
      }
    }

    "not have a message body even when a 204 response with a non-empty body is returned" in withServer(
      Result(header = ResponseHeader(NO_CONTENT),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
      }

    "not have a message body even when a 304 response with a non-empty body is returned" in withServer(
      Result(header = ResponseHeader(NOT_MODIFIED),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
      }

    "not have a message body, nor Content-Length, when a 204 response is returned" in withServer(
      Results.NoContent
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, but may have a Content-Length, when a 204 response with an explicit Content-Length is returned" in withServer(
      Results.NoContent.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beOneOf(None, Some("0")) // Both header values are valid
      }

    "not have a message body, nor a Content-Length, when a 304 response is returned" in withServer(
      Results.NotModified
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, but may have a Content-Length, when a 304 response with an explicit Content-Length is returned" in withServer(
      Results.NotModified.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beOneOf(None, Some("0")) // Both header values are valid
      }

    "return a 500 response if a forbidden character is used in a response's header field" in withServer(
      // both colon and space characters are not allowed in a header's field name
      Results.Ok.withHeaders("BadFieldName: " -> "SomeContent")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.status must_== Status.INTERNAL_SERVER_ERROR
        (response.headers -- Set(CONNECTION, CONTENT_LENGTH, DATE, SERVER)) must be(Map.empty)
      }
  }

  def chunk(content: String) = HttpChunk.Chunk(ByteString(content))
}
