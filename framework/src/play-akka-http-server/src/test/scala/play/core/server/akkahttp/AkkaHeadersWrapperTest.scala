/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.akkahttp

import akka.http.scaladsl.model._
import org.specs2.mutable.Specification
import play.api.http.HeaderNames

class AkkaHeadersWrapperTest extends Specification {
  val emptyRequest: HttpRequest = HttpRequest()

  "AkkaHeadersWrapper" should {
    "return no Content-Type Header when there's not entity (therefore no content type ) in the request" in {
      val request = emptyRequest.copy()
      val headersWrapper = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")

      headersWrapper.headers.find { case (k, _) => k == HeaderNames.CONTENT_TYPE } must be(None)
    }

    "return the appropriate Content-Type Header when there's a request entity" in {
      val plainTextEntity = HttpEntity("Some payload")
      val request = emptyRequest.copy(entity = plainTextEntity)
      val headersWrapper = AkkaHeadersWrapper(request, None, request.headers, None, "some-uri")

      val actualHeaderValue = headersWrapper
        .headers
        .find { case (k, _) => k == HeaderNames.CONTENT_TYPE }
        .get._2
      actualHeaderValue mustEqual "text/plain; charset=UTF-8"
    }

  }
}
