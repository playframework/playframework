/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.encoding

import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.Results.Ok
import play.api.test._

class ContentEncodingFilterSpec extends PlaySpecification {
  "A ContentEncodingFilter" should {
    "encode a response with the supplied flow" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodingName = "test",
        createFlow = () => Flow[ByteString].map(bytes => ByteString(bytes.utf8String.toUpperCase))
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
        encodingName = "test",
        createFlow = () => Flow[ByteString].map(_ => ByteString("encoded"))
      )

      val result = filter(action(Ok("hello")))(FakeRequest()).run()

      header(CONTENT_ENCODING, result) must beNone
      contentAsString(result) must_== "hello"
    }

    "honor the supplied transcoding predicate" in running() { app =>
      implicit val mat = app.materializer
      val action       = app.injector.instanceOf[DefaultActionBuilder]
      val filter       = new ContentEncodingFilter(
        encodingName = "test",
        createFlow = () => Flow[ByteString],
        shouldTranscode = (_, _) => false
      )

      val result = filter(action(Ok("hello")))(FakeRequest().withHeaders(ACCEPT_ENCODING -> "test")).run()

      header(CONTENT_ENCODING, result) must beNone
      contentAsString(result) must_== "hello"
    }
  }
}
