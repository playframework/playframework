package play.filters.gzip

import play.api.test._
import play.api.mvc.{HttpConnection, Action, SimpleResult}
import play.api.mvc.Results._
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils
import scala.concurrent.Future
import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.util.Random

object GzipFilterSpec extends PlaySpecification {

  sequential

  "The GzipFilter" should {

    "gzip responses" in withApplication(Ok("hello")) {
      checkGzippedBody(makeGzipRequest, "hello")
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
  }

  def withApplication[T](result: SimpleResult, buffer: Int = 1024)(block: => T): T = {
    running(FakeApplication(withRoutes = {
      case _ => new GzipFilter(gzip = Gzip.gzip(512), chunkedThreshold = buffer).apply(Action(result))
    }))(block)
  }

  def gzipRequest = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")

  def makeGzipRequest = route(gzipRequest).get

  def gunzip(bytes: Array[Byte]): String = {
    val is = new GZIPInputStream(new ByteArrayInputStream(bytes))
    val result = IOUtils.toString(is, "UTF-8")
    is.close()
    result
  }

  def checkGzipped(result: Future[SimpleResult]) = {
    header(CONTENT_ENCODING, result) must beSome("gzip")
  }

  def checkGzippedBody(result: Future[SimpleResult], body: String) = {
    checkGzipped(result)
    val resultBody = contentAsBytes(result)
    header(CONTENT_LENGTH, result) must beSome(Integer.toString(resultBody.length))
    gunzip(resultBody) must_== body
  }

  def checkNotGzipped(result: Future[SimpleResult], body: String) = {
    header(CONTENT_ENCODING, result) must beNone
    contentAsString(result) must_== body
  }

}
