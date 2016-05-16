/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.gzip

import javax.inject.Inject

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.http.{ HttpEntity, HttpFilters }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router
import play.api.test._
import play.api.mvc.{ Action, Result }
import play.api.mvc.Results._
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils
import scala.concurrent.Future
import scala.util.Random
import org.specs2.matcher.DataTables

object GzipFilterSpec extends PlaySpecification with DataTables {

  sequential

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
      |header fields described in sect 14.1-5.""".stripMargin in withApplication(Ok("meep")) { implicit app =>

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
        "" !! plain |> {

          (codings, expectedEncoding) =>
            header(CONTENT_ENCODING, requestAccepting(app, codings)) must be equalTo (expectedEncoding)
        }
    }

    "not gzip empty responses" in withApplication(Ok) { implicit app =>
      checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "not gzip responses when not requested" in withApplication(Ok("hello")) { implicit app =>
      checkNotGzipped(route(app, FakeRequest()).get, "hello")(app.materializer)
    }

    "not gzip HEAD requests" in withApplication(Ok) { implicit app =>
      checkNotGzipped(route(app, FakeRequest("HEAD", "/").withHeaders(ACCEPT_ENCODING -> "gzip")).get, "")(app.materializer)
    }

    "not gzip no content responses" in withApplication(NoContent) { implicit app =>
      checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "not gzip not modified responses" in withApplication(NotModified) { implicit app =>
      checkNotGzipped(makeGzipRequest(app), "")(app.materializer)
    }

    "gzip chunked responses" in withApplication(Ok.chunked(Source(List("foo", "bar")))) { implicit app =>
      val result = makeGzipRequest(app)
      checkGzippedBody(result, "foobar")(app.materializer)
      await(result).body must beAnInstanceOf[HttpEntity.Chunked]
    }

    val body = Random.nextString(1000)

    "not buffer more than the configured threshold" in withApplication(
      Ok.sendEntity(HttpEntity.Streamed(Source.single(ByteString(body)), Some(1000), None)), chunkedThreshold = 512) { implicit app =>
        val result = makeGzipRequest(app)
        checkGzippedBody(result, body)(app.materializer)
        await(result).body must beAnInstanceOf[HttpEntity.Chunked]
      }

    "zip a strict body even if it exceeds the threshold" in withApplication(Ok(body), 512) { implicit app =>
      val result = makeGzipRequest(app)
      checkGzippedBody(result, body)(app.materializer)
      await(result).body must beAnInstanceOf[HttpEntity.Strict]
    }

    "preserve original headers" in withApplication(Ok("hello").withHeaders(SERVER -> "Play")) { implicit app =>
      val result = makeGzipRequest(app)
      checkGzipped(result)
      header(SERVER, result) must beSome("Play")
    }

    "preserve original Vary header values" in withApplication(Ok("hello").withHeaders(VARY -> "original")) { implicit app =>
      val result = makeGzipRequest(app)
      checkGzipped(result)
      header(VARY, result) must beSome.which(header => header contains "original,")
    }

    "preserve original Vary header values and not duplicate case-insensitive ACCEPT-ENCODING" in withApplication(Ok("hello").withHeaders(VARY -> "original,ACCEPT-encoding")) { implicit app =>
      val result = makeGzipRequest(app)
      checkGzipped(result)
      header(VARY, result) must beSome.which(header => header.split(",").filter(_.toLowerCase(java.util.Locale.ENGLISH) == ACCEPT_ENCODING.toLowerCase(java.util.Locale.ENGLISH)).size == 1)
    }
  }

  class Filters @Inject() (gzipFilter: GzipFilter) extends HttpFilters {
    def filters = Seq(gzipFilter)
  }

  def withApplication[T](result: Result, chunkedThreshold: Int = 1024)(block: Application => T): T = {
    val application = new GuiceApplicationBuilder()
      .configure(
        "play.filters.gzip.chunkedThreshold" -> chunkedThreshold,
        "play.filters.gzip.bufferSize" -> 512
      ).overrides(
          bind[Router].to(Router.from {
            case _ => Action(result)
          }),
          bind[HttpFilters].to[Filters]
        ).build
    running(application)(block(application))
  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")

  def makeGzipRequest(app: Application) = route(app, gzipRequest).get

  def requestAccepting(app: Application, codings: String) = route(app, FakeRequest().withHeaders(ACCEPT_ENCODING -> codings)).get

  def gunzip(bytes: ByteString): String = {
    val is = new GZIPInputStream(new ByteArrayInputStream(bytes.toArray))
    val result = IOUtils.toString(is, "UTF-8")
    is.close()
    result
  }

  def checkGzipped(result: Future[Result]) = {
    header(CONTENT_ENCODING, result) aka "Content encoding header" must beSome("gzip")
  }

  def checkGzippedBody(result: Future[Result], body: String)(implicit mat: Materializer) = {
    checkGzipped(result)
    val resultBody = contentAsBytes(result)
    await(result).body.contentLength.foreach { cl =>
      resultBody.length must_== cl
    }
    gunzip(resultBody) must_== body
  }

  def checkNotGzipped(result: Future[Result], body: String)(implicit mat: Materializer) = {
    header(CONTENT_ENCODING, result) must beNone
    contentAsString(result) must_== body
  }
}
