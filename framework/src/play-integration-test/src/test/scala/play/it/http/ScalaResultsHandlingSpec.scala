/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.nio.file.{ Path, Files => JFiles }
import java.util.Locale.ENGLISH

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.libs.ws._
import play.api.libs.EventSource
import play.core.server.common.ServerResultException
import play.it._

import scala.util.Try
import scala.concurrent.Future
import play.api.http.{ HttpChunk, HttpEntity }

class NettyScalaResultsHandlingSpec extends ScalaResultsHandlingSpec with NettyIntegrationSpecification
class AkkaHttpScalaResultsHandlingSpec extends ScalaResultsHandlingSpec with AkkaHttpIntegrationSpecification

trait ScalaResultsHandlingSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  "scala result handling" should {

    def tryRequest[T](result: => Result)(block: Try[WSResponse] => T) = withServer(result) { implicit port =>
      val response = Try(await(wsUrl("/").get()))
      block(response)
    }

    def makeRequest[T](result: => Result)(block: WSResponse => T) = {
      tryRequest(result)(tryResult => block(tryResult.get))
    }

    def withServer[T](result: => Result, errorHandler: HttpErrorHandler = DefaultHttpErrorHandler)(block: play.api.test.Port => T) = {
      val port = testServerPort
      val app = GuiceApplicationBuilder()
        .overrides(bind[HttpErrorHandler].to(errorHandler))
        .routes { case _ => ActionBuilder.ignoringBody(result) }
        .build()
      running(TestServer(port, app)) {
        block(port)
      }
    }

    "add Date header" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(DATE) must beSome
    }

    "work with non-standard HTTP response codes" in makeRequest(Result(ResponseHeader(498), HttpEntity.NoEntity)) { response =>
      response.status must_== 498
      response.body must beEmpty
    }

    "add Content-Length for strict results" in makeRequest(Results.Ok("Hello world")) { response =>
      response.header(CONTENT_LENGTH) must beSome("11")
      response.body must_== "Hello world"
    }

    def emptyStreamedEntity = Results.Ok.sendEntity(HttpEntity.Streamed(Source.empty[ByteString], Some(0), None))

    "not fail when sending an empty entity with a known size zero" in makeRequest(emptyStreamedEntity) {
      response =>
        response.status must_== 200
        response.header(CONTENT_LENGTH) must beSome("0") or beNone
    }

    "not fail when sending an empty file" in {
      val emptyPath = JFiles.createTempFile("empty", ".txt")
      // todo fix the ExecutionContext. Not sure where to get it from nicely
      // maybe the test is in the wrong place
      import scala.concurrent.ExecutionContext.Implicits.global
      // todo not sure where to get this one from in this context, either
      implicit val fileMimeTypes = new FileMimeTypes {
        override def forFileName(name: String): Option[String] = Some("text/plain")
      }
      try makeRequest(
        Results.Ok.sendPath(emptyPath)
      ) {
          response =>
            response.status must_== 200
            response.header(CONTENT_LENGTH) must beSome("0")
        } finally JFiles.delete(emptyPath)
    }

    "not add a content length header when none is supplied" in makeRequest(
      Results.Ok.sendEntity(HttpEntity.Streamed(Source(List("abc", "def", "ghi")).map(ByteString.apply), None, None))
    ) { response =>
        response.header(CONTENT_LENGTH) must beNone
        response.header(TRANSFER_ENCODING) must beNone
        response.body must_== "abcdefghi"
      }

    "support responses with custom Content-Types" in {
      makeRequest(
        Results.Ok.sendEntity(HttpEntity.Strict(ByteString(0xff.toByte), Some("schmitch/foo; bar=bax")))
      ) { response =>
          response.header(CONTENT_TYPE) must beSome("schmitch/foo; bar=bax")
          response.header(CONTENT_LENGTH) must beSome("1")
          response.header(TRANSFER_ENCODING) must beNone
          response.bodyAsBytes must_== ByteString(0xff.toByte)
        }
    }

    "support multipart/mixed responses" in {
      // Example taken from https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
      val contentType = "multipart/mixed; boundary=\"simple boundary\""
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
      makeRequest(
        Results.Ok.sendEntity(HttpEntity.Strict(ByteString(body), Some(contentType)))
      ) { response =>
          response.header(CONTENT_TYPE) must beSome(contentType)
          response.header(CONTENT_LENGTH) must beSome(body.length.toString)
          response.header(TRANSFER_ENCODING) must beNone
          response.body must_== body
        }
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
      Result(
        ResponseHeader(200, Map(TRANSFER_ENCODING -> CHUNKED, TRAILER -> "Chunks")),
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
      Results.Ok.chunked(Source(List("a", "b", "c"))),
      errorHandler = new HttpErrorHandler {
        override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = ???
        override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
          request.path must_== "/"
          exception must beLike {
            case e: ServerResultException =>
              // Check original result
              e.result.header.status must_== 200
          }
          Future.successful(Results.Status(500))
        }
      }
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.0", Map(), "")
        ).head
        response.status must_== 505
      }

    "return a 500 error on response with null header" in withServer(
      Results.Ok("some body").withHeaders("X-Null" -> null)
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        response.status must_== 500
        response.body must beLeft
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

    "support UTF-8 encoded filenames in Content-Disposition headers" in {
      val tempFile: Path = JFiles.createTempFile("ScalaResultsHandlingSpec", "txt")
      try {
        withServer {
          import scala.concurrent.ExecutionContext.Implicits.global
          implicit val mimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration())
          Results.Ok.sendFile(
            tempFile.toFile,
            fileName = _ => "测 试.tmp"
          )
        } { port =>
          val response = BasicHttpClient.makeRequests(port)(
            BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
          ).head

          response.status must_== 200
          response.body must beLeft("")
          response.headers.get(CONTENT_DISPOSITION) must beSome(s"""inline; filename="? ?.tmp"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp""")
        }
      } finally {
        tempFile.toFile.delete()
      }
    }

    "split Set-Cookie headers" in {
      import play.api.mvc.Cookie

      lazy val cookieHeaderEncoding = new DefaultCookieHeaderEncoding()

      val aCookie = Cookie("a", "1")
      val bCookie = Cookie("b", "2")
      val cCookie = Cookie("c", "3")
      makeRequest {
        Results.Ok.withCookies(aCookie, bCookie, cCookie)
      } { response =>
        response.headers.get(SET_COOKIE) must beSome.like {
          case rawCookieHeaders =>
            val decodedCookieHeaders: Set[Set[Cookie]] = rawCookieHeaders.map { headerValue =>
              cookieHeaderEncoding.decodeSetCookieHeader(headerValue).to[Set]
            }.to[Set]
            decodedCookieHeaders must_== (Set(Set(aCookie), Set(bCookie), Set(cCookie)))
        }
      }
    }

    "not have a message body even when a 100 response with a non-empty body is returned" in withServer(
      Result(
        header = ResponseHeader(CONTINUE),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body even when a 101 response with a non-empty body is returned" in withServer(
      Result(
        header = ResponseHeader(SWITCHING_PROTOCOLS),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body even when a 204 response with a non-empty body is returned" in withServer(
      Result(
        header = ResponseHeader(NO_CONTENT),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body even when a 304 response with a non-empty body is returned" in withServer(
      Result(
        header = ResponseHeader(NOT_MODIFIED),
        body = HttpEntity.Strict(ByteString("foo"), None)
      )
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
      }

    "not have a message body, nor Content-Length, when a 100 response is returned" in withServer(
      Results.Continue
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, nor Content-Length, when a 101 response is returned" in withServer(
      Results.SwitchingProtocols
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
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

    "not have a message body, nor Content-Length, when a 304 response is returned" in withServer(
      Results.NotModified
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, nor Content-Length, even when a 100 response with an explicit Content-Length is returned" in withServer(
      Results.Continue.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, nor Content-Length, even when a 101 response with an explicit Content-Length is returned" in withServer(
      Results.SwitchingProtocols.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, nor Content-Length, even when a 204 response with an explicit Content-Length is returned" in withServer(
      Results.NoContent.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("PUT", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "not have a message body, nor Content-Length, even when a 304 response with an explicit Content-Length is returned" in withServer(
      Results.NotModified.withHeaders("Content-Length" -> "0")
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.body must beLeft("")
        response.headers.get(CONTENT_LENGTH) must beNone
      }

    "return a 500 response if a forbidden character is used in a response's header field" in withServer(
      // both colon and space characters are not allowed in a header's field name
      Results.Ok.withHeaders("BadFieldName: " -> "SomeContent"),
      errorHandler = new HttpErrorHandler {
        override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = ???
        override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
          request.path must_== "/"
          exception must beLike {
            case e: ServerResultException =>
              // Check original result
              e.result.header.status must_== 200
              e.result.header.headers.get("BadFieldName: ") must beSome("SomeContent")
          }
          Future.successful(Results.Status(500))
        }
      }
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.status must_== 500
        (response.headers -- Set(CONNECTION, CONTENT_LENGTH, DATE, SERVER)) must be empty
      }

    "return a 500 response if an error occurs during the onError" in withServer(
      // both colon and space characters are not allowed in a header's field name
      Results.Ok.withHeaders("BadFieldName: " -> "SomeContent"),
      errorHandler = new HttpErrorHandler {
        override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = ???
        override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
          throw new Exception("Failing on purpose :)")
        }
      }
    ) { port =>
        val response = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head
        response.status must_== 500
        (response.headers -- Set(CONNECTION, CONTENT_LENGTH, DATE, SERVER)) must be empty
      }
  }

  def chunk(content: String) = HttpChunk.Chunk(ByteString(content))
}
