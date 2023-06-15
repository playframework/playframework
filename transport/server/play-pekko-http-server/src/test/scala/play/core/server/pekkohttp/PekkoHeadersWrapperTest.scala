/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.akkahttp

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import org.specs2.mutable.Specification
import play.api.http.HeaderNames

class AkkaHeadersWrapperTest extends Specification {
  val emptyRequest: HttpRequest = HttpRequest()

  "AkkaHeadersWrapper" should {
    "return no Content-Type Header when there's not entity (therefore no content type ) in the request" in {
      val request        = emptyRequest.copy()
      val headersWrapper = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")

      headersWrapper.headers.find { case (k, _) => k == HeaderNames.CONTENT_TYPE } must be(None)
    }

    "return the appropriate Content-Type Header when there's a request entity" in {
      val plainTextEntity = HttpEntity("Some payload")
      val request         = emptyRequest.withEntity(entity = plainTextEntity)
      val headersWrapper  = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")

      val actualHeaderValue = headersWrapper.headers
        .find { case (k, _) => k == HeaderNames.CONTENT_TYPE }
        .get
        ._2
      actualHeaderValue mustEqual "text/plain; charset=UTF-8"
    }

    "remove a header" in {
      val name            = "my-private-header"
      val plainTextEntity = HttpEntity("Some payload")
      val headers         = scala.collection.immutable.Seq(RawHeader(name, "asdf"))
      val request         = emptyRequest.withEntity(entity = plainTextEntity).withHeaders(headers = headers)
      val headersWrapper  = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")
      headersWrapper(name) mustEqual "asdf"

      val cleaned = headersWrapper.remove(name)
      cleaned.get(name) must beNone
    }

    "remove the Content-Type header" in {
      val plainTextEntity = HttpEntity("Some payload")
      val request         = emptyRequest.withEntity(entity = plainTextEntity)
      val headersWrapper  = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")
      headersWrapper(HeaderNames.CONTENT_TYPE) mustEqual "text/plain; charset=UTF-8"

      val cleaned = headersWrapper.remove(HeaderNames.CONTENT_TYPE)
      cleaned.get(HeaderNames.CONTENT_TYPE) must beNone
    }

    "remove the Content-Length header" in {
      val plainTextEntity = HttpEntity("Some payload")
      val request         = emptyRequest.withEntity(entity = plainTextEntity)
      val headersWrapper =
        AkkaHeadersWrapper(request, Some(plainTextEntity.contentLength.toString), request.headers, None, "some-uri")
      headersWrapper(HeaderNames.CONTENT_LENGTH) mustEqual plainTextEntity.contentLength.toString

      val cleaned = headersWrapper.remove(HeaderNames.CONTENT_LENGTH)
      cleaned.get(HeaderNames.CONTENT_LENGTH) must beNone
    }
  }
}
