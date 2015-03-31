/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.gzip

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router
import play.api.test._
import play.api.mvc.{ HttpConnection, Action, Result }
import play.api.mvc.Results._
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils
import scala.concurrent.Future
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import scala.util.Random
import org.specs2.matcher.DataTables

object GzipFilterSpec extends PlaySpecification with DataTables {

  sequential

  "The GzipFilter" should {

    "gzip responses" in withApplication(Ok("hello")) {
      checkGzippedBody(makeGzipRequest, "hello")
    }

    """gzip a response if (and only if) it is accepted and preferred by the request.
      |Although not explicitly mentioned in RFC 2616 sect. 14.3, the default qvalue
      |is assumed to be 1 for all mentioned codings. If no "*" is present, unmentioned
      |codings are assigned a qvalue of 0, except the identity coding which gets q=0.001,
      |which is the lowest possible acceptable qvalue.
      |This seems to be the most consistent behaviour with respect to the other "accept"
      |header fields described in sect 14.1-5.""".stripMargin in withApplication(Ok("meep")) {

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
            header(CONTENT_ENCODING, requestAccepting(codings)) must be equalTo (expectedEncoding)
        }
    }

    "not gzip responses when not requested" in withApplication(Ok("hello")) {
      checkNotGzipped(route(FakeRequest()).get, "hello")
    }

    "not gzip HEAD requests" in withApplication(Ok) {
      checkNotGzipped(route(FakeRequest("HEAD", "/").withHeaders(ACCEPT_ENCODING -> "gzip")).get, "")
    }

    "not gzip no content responses" in withApplication(NoContent) {
      checkNotGzipped(makeGzipRequest, "")
    }

    "not gzip not modified responses" in withApplication(NotModified) {
      checkNotGzipped(makeGzipRequest, "")
    }

    "not gzip chunked responses" in withApplication(Ok.chunked(Enumerator("foo", "bar"))) {
      val result = makeGzipRequest
      header(CONTENT_ENCODING, result) must beNone
      header(CONTENT_LENGTH, result) must beNone
      header(TRANSFER_ENCODING, result) must beSome("chunked")
      new String(await(await(result).body &> dechunk |>>> Iteratee.consume[Array[Byte]]()), "UTF-8") must_== "foobar"
    }

    "not gzip event stream responses" in withApplication(Ok.feed(Enumerator("foo", "bar")).as("text/event-stream")) {
      checkNotGzipped(makeGzipRequest, "foobar")
    }

    "filter content length headers" in withApplication(
      Ok("hello")
        .withHeaders(CONTENT_LENGTH -> Integer.toString(328974))
        // http connection close will trigger the gzip filter not to buffer
        .copy(connection = HttpConnection.Close)
    ) {
        val result = makeGzipRequest
        checkGzipped(result)
        header(CONTENT_LENGTH, result) must beNone
      }

    val body = Random.nextString(1000)

    "not buffer more than the configured threshold" in withApplication(Ok(body).withHeaders(CONTENT_LENGTH -> "1000")) {
      val result = makeGzipRequest
      checkGzipped(result)
      header(CONTENT_LENGTH, result) must beNone
      header(TRANSFER_ENCODING, result) must beSome("chunked")
      gunzip(await(await(result).body &> dechunk |>>> Iteratee.consume[Array[Byte]]())) must_== body
    }

    "buffer the first chunk even if it exceeds the threshold" in withApplication(Ok("foobarblah"), 15) {
      checkGzippedBody(makeGzipRequest, "foobarblah")
    }

    "preserve original headers" in withApplication(Redirect("/foo")) {
      val result = makeGzipRequest
      checkGzipped(result)
      header(LOCATION, result) must beSome("/foo")
    }

    "preserve original Vary header values" in withApplication(Ok("hello").withHeaders(VARY -> "original")) {
      val result = makeGzipRequest
      checkGzipped(result)
      header(VARY, result) must beSome.which(header => header contains "original,")
    }
  }

  class Filters @Inject() (gzipFilter: GzipFilter) extends HttpFilters {
    def filters = Seq(gzipFilter)
  }

  def withApplication[T](result: Result, chunkedThreshold: Int = 1024)(block: => T): T = {
    running(new GuiceApplicationBuilder()
      .configure(
        "play.filters.gzip.chunkedThreshold" -> chunkedThreshold,
        "play.filters.gzip.bufferSize" -> 512
      ).overrides(
          bind[Router].to(Router.from {
            case _ => Action(result)
          }),
          bind[HttpFilters].to[Filters]
        ).build()
    )(block)
  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")

  def makeGzipRequest = route(gzipRequest).get

  def requestAccepting(codings: String) = route(FakeRequest().withHeaders(ACCEPT_ENCODING -> codings)).get

  def gunzip(bytes: Array[Byte]): String = {
    val is = new GZIPInputStream(new ByteArrayInputStream(bytes))
    val result = IOUtils.toString(is, "UTF-8")
    is.close()
    result
  }

  def checkGzipped(result: Future[Result]) = {
    header(CONTENT_ENCODING, result) must beSome("gzip")
  }

  def checkGzippedBody(result: Future[Result], body: String) = {
    checkGzipped(result)
    val resultBody = contentAsBytes(result)
    header(CONTENT_LENGTH, result) must beSome(Integer.toString(resultBody.length))
    gunzip(resultBody) must_== body
  }

  def checkNotGzipped(result: Future[Result], body: String) = {
    header(CONTENT_ENCODING, result) must beNone
    contentAsString(result) must_== body
  }
}
