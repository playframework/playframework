/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.gzip

import javax.inject.Inject
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.http.{ HttpChunk, HttpEntity, HttpFilters, HttpProtocol }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.{ Router, SimpleRouterImpl }
import play.api.test._
import play.api.mvc.{ AnyContentAsEmpty, Cookie, DefaultActionBuilder, Result }
import play.api.mvc.Results._
import java.util.zip.{ Deflater, GZIPInputStream }
import java.io.{ ByteArrayInputStream, InputStreamReader }

import com.google.common.io.CharStreams

import scala.concurrent.Future
import scala.util.Random
import org.specs2.matcher.{ DataTables, MatchResult }

object GzipFilterSpec {
  class ResultRouter @Inject() (action: DefaultActionBuilder, result: Result)
    extends SimpleRouterImpl({ case _ => action(result) })

  class Filters @Inject() (gzipFilter: GzipFilter) extends HttpFilters {
    def filters = Seq(gzipFilter)
  }

}

class GzipFilterSpec extends PlaySpecification with DataTables {

  sequential

  import GzipFilterSpec._

  "The GzipFilter" should {

    "gzip responses" in withApplication(Ok("hello")) { implicit app =>
      checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
    }

    """gzip a response if (and only if) it is accepted and preferred by the request.
      |Although not explicitly mentioned in RFC 2616 sect. 14.3, the default qvalue
      |is assumed to be 1 for all mentioned codings. If no "*" is present, unmentioned
      |codings are assigned a qvalue of 0, except the identity coding which gets q=0.001,
      |which is the lowest possible acceptable qvalue.
      |This seems to be the most consistent behaviour with respect to the other "accept"
      |header fields described in sect 14.1-5.""".stripMargin in withApplication(
      Ok("meep")) { implicit app =>
        val (plain, gzipped) = (None, Some("gzip"))

        "Accept-Encoding of request" || "Response" |
          //------------------------------------++------------+
          "gzip" !! gzipped |
          "compress,gzip" !! gzipped |
          "compress, gzip" !! gzipped |
          "gzip,compress" !! gzipped |
          "deflate, gzip,compress" !! gzipped |
          "gzip, compress" !! gzipped |
          "identity, gzip, compress" !! gzipped |
          "GZip" !! gzipped |
          "*" !! gzipped |
          "*;q=0" !! plain |
          "*; q=0" !! plain |
          "*;q=0.000" !! plain |
          "gzip;q=0" !! plain |
          "gzip; q=0.00" !! plain |
          "*;q=0, gZIP" !! gzipped |
          "compress;q=0.1, *;q=0, gzip" !! gzipped |
          "compress;q=0.1, *;q=0, gzip;q=0.005" !! gzipped |
          "compress, gzip;q=0.001" !! gzipped |
          "compress, gzip;q=0.002" !! gzipped |
          "compress;q=1, *;q=0, gzip;q=0.000" !! plain |
          "compress;q=1, *;q=0" !! plain |
          "identity" !! plain |
          "gzip;q=0.5, identity" !! plain |
          "gzip;q=0.5, identity;q=1" !! plain |
          "gzip;q=0.6, identity;q=0.5" !! gzipped |
          "*;q=0.7, gzip;q=0.6, identity;q=0.4" !! gzipped |
          "" !! plain |> { (codings, expectedEncoding) =>
            header(CONTENT_ENCODING, requestAccepting(app, codings)) must be equalTo expectedEncoding
          }
      }

    "not gzip empty responses" in withApplication(Ok) { implicit app =>
      checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "not gzip responses when not requested" in withApplication(Ok("hello")) {
      implicit app =>
        checkNotGzipped(route(app, FakeRequest()).get, "hello")(
          app.materializer)
    }

    "not gzip HEAD requests" in withApplication(Ok) { implicit app =>
      checkNotGzipped(
        route(
          app,
          FakeRequest("HEAD", "/").withHeaders(
            ACCEPT_ENCODING -> "gzip")).get,
        "")(app.materializer)
    }

    "not gzip no content responses" in withApplication(NoContent) {
      implicit app =>
        checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "not gzip not modified responses" in withApplication(NotModified) {
      implicit app =>
        checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "gzip content type which is on the whiteList" in withApplication(
      Ok("hello").as("text/css"),
      whiteList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip content type which is on the whiteList ignoring case" in withApplication(
      Ok("hello").as("TeXt/CsS"),
      whiteList = List("TExT/HtMl", "tExT/cSs")) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip uppercase content type which is on the whiteList" in withApplication(
      Ok("hello").as("TEXT/CSS"),
      whiteList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip content type with charset which is on the whiteList" in withApplication(
      Ok("hello").as("text/css; charset=utf-8"),
      whiteList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "don't gzip content type which is not on the whiteList" in withApplication(
      Ok("hello").as("text/plain"),
      whiteList = contentTypes) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "don't gzip content type with charset which is not on the whiteList" in withApplication(
      Ok("hello").as("text/plain; charset=utf-8"),
      whiteList = contentTypes) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "don't gzip content type which is on the blackList" in withApplication(
      Ok("hello").as("text/css"),
      blackList = contentTypes) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "don't gzip content type with charset which is on the blackList" in withApplication(
      Ok("hello").as("text/css; charset=utf-8"),
      blackList = contentTypes) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip content type which is not on the blackList" in withApplication(
      Ok("hello").as("text/plain"),
      blackList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip content type with charset which is not on the blackList" in withApplication(
      Ok("hello").as("text/plain; charset=utf-8"),
      blackList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "ignore blackList if there is a whiteList" in withApplication(
      Ok("hello").as("text/css; charset=utf-8"),
      whiteList = contentTypes,
      blackList = contentTypes) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip 'text/html' content type when using media range 'text/*' in the whiteList" in withApplication(
      Ok("hello").as("text/css"),
      whiteList = List("text/*")) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "don't gzip 'application/javascript' content type when using media range 'text/*' in the whiteList" in withApplication(
      Ok("hello").as("application/javascript"),
      whiteList = List("text/*")) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "fail closed to not gziping an invalid contentType if there is a whiteList and no blacklist" in withApplication(
      Ok("hello").as("aA(\\A@*- 1  a-"),
      whiteList = List("text/*")) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "fail closed to not gziping an invalid contentType if there is a whiteList and a blacklist" in withApplication(
      Ok("hello").as("aA(\\A@*- 1  a-"),
      whiteList = List("text/*"),
      blackList = List("text/*")) { implicit app =>
        checkNotGzipped(makeGzipRequest(app), "hello")(app.materializer)
      }

    "fail opened to gziping an invalid contentType if there is a blacklist and no whitelist" in withApplication(
      Ok("hello").as("aA(\\A@*- 1  a-"),
      blackList = List("text/*")) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip an invalid contentType if there is neither a blacklist nor a whitelist" in withApplication(
      Ok("hello").as("aA(\\A@*- 1  a-")) { implicit app =>
        checkGzippedBody(makeGzipRequest(app), "hello")(app.materializer)
      }

    "gzip chunked responses" in withApplication(
      Ok.chunked(Source(List("foo", "bar")))) { implicit app =>
        val result = makeGzipRequest(app)
        checkGzippedBody(result, "foobar")(app.materializer)
        await(result).body must beAnInstanceOf[HttpEntity.Chunked]
      }

    val body = Random.nextString(1000)

    "a streamed body" should {

      val entity =
        HttpEntity.Streamed(Source.single(ByteString(body)), Some(1000), None)

      "not buffer more than the configured threshold" in withApplication(
        Ok.sendEntity(entity),
        chunkedThreshold = 512) { implicit app =>
          val result = makeGzipRequest(app)
          checkGzippedBody(result, body)(app.materializer)
          await(result).body must beAnInstanceOf[HttpEntity.Chunked]
        }

      "preserve original headers, cookies, flash and session values" in {

        "when buffer is less than configured threshold" in withApplication(
          Ok.sendEntity(entity)
            .withHeaders(SERVER -> "Play")
            .withCookies(Cookie("cookieName", "cookieValue"))
            .flashing("flashName" -> "flashValue")
            .withSession("sessionName" -> "sessionValue"),
          chunkedThreshold = 2048 // body size is 1000
        ) { implicit app =>
            val result = makeGzipRequest(app)
            checkGzipped(result)
            header(SERVER, result) must beSome("Play")
            cookies(result).get("cookieName") must beSome.which(cookie =>
              cookie.value == "cookieValue")
            flash(result).get("flashName") must beSome.which(value =>
              value == "flashValue")
            session(result).get("sessionName") must beSome.which(value =>
              value == "sessionValue")
          }

        "when buffer more than configured threshold" in withApplication(
          Ok.sendEntity(entity)
            .withHeaders(SERVER -> "Play")
            .withCookies(Cookie("cookieName", "cookieValue"))
            .flashing("flashName" -> "flashValue")
            .withSession("sessionName" -> "sessionValue"),
          chunkedThreshold = 512
        ) { implicit app =>
            val result = makeGzipRequest(app)
            checkGzippedBody(result, body)(app.materializer)
            header(SERVER, result) must beSome("Play")
            cookies(result).get("cookieName") must beSome.which(cookie =>
              cookie.value == "cookieValue")
            flash(result).get("flashName") must beSome.which(value =>
              value == "flashValue")
            session(result).get("sessionName") must beSome.which(value =>
              value == "sessionValue")
          }
      }

      "not fallback to a chunked body when HTTP 1.0 is being used and the chunked threshold is exceeded" in withApplication(
        Ok.sendEntity(entity),
        chunkedThreshold = 512) { implicit app =>
          val result =
            route(app, gzipRequest.withVersion(HttpProtocol.HTTP_1_0)).get
          checkGzippedBody(result, body)(app.materializer)
          val entity = await(result).body
          entity must beLike {
            // Make sure it's a streamed entity with no content length
            case HttpEntity.Streamed(_, None, None) => ok
          }

        }
    }

    "a chunked body" should {
      val chunkedBody = Source.fromIterator(
        () =>
          Seq[HttpChunk](
            HttpChunk.Chunk(ByteString("First chunk")),
            HttpChunk.LastChunk(FakeHeaders())).iterator)

      val entity = HttpEntity.Chunked(chunkedBody, Some("text/plain"))

      "preserve original headers, cookie, flash and session values" in withApplication(
        Ok.sendEntity(entity)
          .withHeaders(SERVER -> "Play")
          .withCookies(Cookie("cookieName", "cookieValue"))
          .flashing("flashName" -> "flashValue")
          .withSession("sessionName" -> "sessionValue")
      ) { implicit app =>
          val result = makeGzipRequest(app)
          checkGzipped(result)
          header(SERVER, result) must beSome("Play")
          cookies(result).get("cookieName") must beSome.which(cookie =>
            cookie.value == "cookieValue")
          flash(result).get("flashName") must beSome.which(value =>
            value == "flashValue")
          session(result).get("sessionName") must beSome.which(value =>
            value == "sessionValue")
        }
    }

    "a strict body" should {

      "zip a strict body even if it exceeds the threshold" in withApplication(
        Ok(body),
        512) { implicit app =>
          val result = makeGzipRequest(app)
          checkGzippedBody(result, body)(app.materializer)
          await(result).body must beAnInstanceOf[HttpEntity.Strict]
        }

      "preserve original headers, cookie, flash and session values" in withApplication(
        Ok("hello")
          .withHeaders(SERVER -> "Play")
          .withCookies(Cookie("cookieName", "cookieValue"))
          .flashing("flashName" -> "flashValue")
          .withSession("sessionName" -> "sessionValue")
      ) { implicit app =>
          val result = makeGzipRequest(app)
          checkGzipped(result)
          header(SERVER, result) must beSome("Play")
          cookies(result).get("cookieName") must beSome.which(cookie =>
            cookie.value == "cookieValue")
          flash(result).get("flashName") must beSome.which(value =>
            value == "flashValue")
          session(result).get("sessionName") must beSome.which(value =>
            value == "sessionValue")
        }

      "preserve original Vary header values" in withApplication(
        Ok("hello").withHeaders(VARY -> "original")) { implicit app =>
          val result = makeGzipRequest(app)
          checkGzipped(result)
          header(VARY, result) must beSome.which(header =>
            header contains "original,")
        }

      "preserve original Vary header values and not duplicate case-insensitive ACCEPT-ENCODING" in withApplication(
        Ok("hello").withHeaders(VARY -> "original,ACCEPT-encoding")) {
          implicit app =>
            val result = makeGzipRequest(app)
            checkGzipped(result)
            header(VARY, result) must beSome.which(
              header =>
                header
                  .split(",")
                  .count(
                    _.toLowerCase(java.util.Locale.ENGLISH) == ACCEPT_ENCODING
                      .toLowerCase(java.util.Locale.ENGLISH)) == 1)
        }
    }

    // Random output doesn't compress well and makes for an unreliable comparison between compression levels. This
    // admittedly way too complicated way to create a test string is somewhat compressible and identical run-to-run.
    val compressibleBody: String = {
      var i = 0
      var j = 0
      var count = 0
      val N = 25000
      val sb = new java.lang.StringBuilder(N + 100)
      val alphabet = "abcdefghijklmnopqrstuvwxyz012"
      while (count < N) {
        j = (j + 1) % alphabet.length
        val char = alphabet.charAt(j)
        i = (i + 7) % 17
        for (x <- 0 until i) sb.append(char)
        count += i
      }
      sb.toString
    }
    "GzipFilterConfig.compressionLevel" should {
      "changing the compressionLevel should result in a change in the output size" in {
        val result1 =
          withApplication(Ok(compressibleBody), compressionLevel = 1) {
            implicit app =>
              contentAsBytes(makeGzipRequest(app))
          }
        val result9 =
          withApplication(Ok(compressibleBody), compressionLevel = 9) {
            implicit app =>
              contentAsBytes(makeGzipRequest(app))
          }
        result1.length should be > result9.length
      }

      "NOT changing the compressionLevel should NOT result in a change in the output size" in {
        val result1a =
          withApplication(Ok(compressibleBody), compressionLevel = 1) {
            implicit app =>
              contentAsBytes(makeGzipRequest(app))
          }
        val result1b =
          withApplication(Ok(compressibleBody), compressionLevel = 1) {
            implicit app =>
              contentAsBytes(makeGzipRequest(app))
          }
        result1a.length === result1b.length
      }
    }
  }

  def withApplication[T](
    result: Result,
    chunkedThreshold: Int = 1024,
    whiteList: List[String] = List.empty,
    blackList: List[String] = List.empty,
    compressionLevel: Int = Deflater.DEFAULT_COMPRESSION)(
    block: Application => T): T = {
    val application = new GuiceApplicationBuilder()
      .configure(
        "play.filters.gzip.chunkedThreshold" -> chunkedThreshold,
        "play.filters.gzip.bufferSize" -> 512,
        "play.filters.gzip.contentType.whiteList" -> whiteList,
        "play.filters.gzip.contentType.blackList" -> blackList,
        "play.filters.gzip.compressionLevel" -> compressionLevel
      )
      .overrides(
        bind[Result].to(result),
        bind[Router].to[ResultRouter],
        bind[HttpFilters].to[Filters]
      )
      .build()
    running(application)(block(application))
  }

  val contentTypes = List("text/html", "text/css", "application/javascript")

  def gzipRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")

  def makeGzipRequest(app: Application): Future[Result] =
    route(app, gzipRequest).get

  def requestAccepting(app: Application, codings: String): Future[Result] =
    route(app, FakeRequest().withHeaders(ACCEPT_ENCODING -> codings)).get

  def gunzip(bytes: ByteString): String = {
    val is = new GZIPInputStream(new ByteArrayInputStream(bytes.toArray))
    val reader = new InputStreamReader(is, "UTF-8")
    try CharStreams.toString(reader)
    finally reader.close()
  }

  def checkGzipped(result: Future[Result]): MatchResult[Option[String]] = {
    header(CONTENT_ENCODING, result) aka "Content encoding header" must beSome(
      "gzip")
  }

  def checkGzippedBody(result: Future[Result], body: String)(
    implicit
    mat: Materializer): MatchResult[Any] = {
    checkGzipped(result)
    val resultBody = contentAsBytes(result)
    await(result).body.contentLength.foreach { cl =>
      resultBody.length must_== cl
    }
    gunzip(resultBody) must_== body
  }

  def checkNotGzipped(result: Future[Result], body: String)(
    implicit
    mat: Materializer): MatchResult[Any] = {
    header(CONTENT_ENCODING, result) must beNone
    contentAsString(result) must_== body
  }
}
