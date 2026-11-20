/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.encoding

import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.Results.Ok
import play.api.test._
import play.filters.encoding.ContentEncodingFilter.Encoding

class ContentEncodingFilterSpec extends PlaySpecification {
  "A ContentEncodingFilter" should {
    "encode a response with the supplied flow" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodings = Seq(Encoding("test", () => Flow[ByteString].map(bytes => ByteString(bytes.utf8String.toUpperCase))))
      )

      val result = filter(action(Ok("hello")))(FakeRequest().withHeaders(ACCEPT_ENCODING -> "test")).run()

      header(CONTENT_ENCODING, result) must beSome("test")
      header(VARY, result) must beSome(ACCEPT_ENCODING)
      contentAsString(result) must_== "HELLO"
    }

    "leave a response unchanged when the encoding is not accepted" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodings = Seq(Encoding("test", () => Flow[ByteString].map(_ => ByteString("encoded"))))
      )

      val result = filter(action(Ok("hello")))(FakeRequest()).run()

      header(CONTENT_ENCODING, result) must beNone
      contentAsString(result) must_== "hello"
    }

    "honor the supplied transcoding predicate" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodings = Seq(Encoding("test", () => Flow[ByteString])),
        shouldTranscode = (_, _) => false
      )

      val result = filter(action(Ok("hello")))(FakeRequest().withHeaders(ACCEPT_ENCODING -> "test")).run()

      header(CONTENT_ENCODING, result) must beNone
      contentAsString(result) must_== "hello"
    }

    "use the encoding most preferred by the client" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodings = Seq(
          Encoding("gzip", () => Flow[ByteString].map(_ => ByteString("gzip"))),
          Encoding("zstd", () => Flow[ByteString].map(_ => ByteString("zstd")))
        )
      )

      val result = filter(action(Ok("hello")))(
        FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip;q=0.5, zstd;q=1")
      ).run()

      header(CONTENT_ENCODING, result) must beSome("zstd")
      contentAsString(result) must_== "zstd"
    }

    "use server preference order when accepted encodings have equal quality" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodings = Seq(
          Encoding("zstd", () => Flow[ByteString].map(_ => ByteString("zstd"))),
          Encoding("gzip", () => Flow[ByteString].map(_ => ByteString("gzip")))
        )
      )

      val result = filter(action(Ok("hello")))(FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip, zstd")).run()

      header(CONTENT_ENCODING, result) must beSome("zstd")
      contentAsString(result) must_== "zstd"
    }
  }
}
