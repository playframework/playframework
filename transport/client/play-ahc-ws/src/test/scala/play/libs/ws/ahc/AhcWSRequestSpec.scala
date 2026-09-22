/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc

import java.util.concurrent.CompletableFuture

import org.apache.pekko.util.ByteString
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.specs2.mutable.Specification
import play.libs.ws.BodyWritable
import play.libs.ws.StandaloneWSResponse
import play.libs.ws.WSResponse

class AhcWSRequestSpec extends Specification {

  "AhcWSRequest.query(String)" should {
    "send a QUERY request with a body and return a Play WS response" in {
      val standalone       = Mockito.mock(classOf[StandaloneAhcWSRequest])
      val underlyingResult = Mockito.mock(classOf[StandaloneWSResponse])
      Mockito
        .doReturn(CompletableFuture.completedFuture(underlyingResult))
        .when(standalone)
        .query(ArgumentMatchers.any[BodyWritable[?]])

      val request                               = new AhcWSRequest(null, standalone)
      val result: CompletableFuture[WSResponse] = request.query("query body").toCompletableFuture

      val body = ArgumentCaptor.forClass(classOf[BodyWritable[?]])
      Mockito.verify(standalone).query(body.capture())

      result.get() must beAnInstanceOf[AhcWSResponse]
      body.getValue.contentType must_== "text/plain"
      body.getValue.body().get().asInstanceOf[ByteString].utf8String must_== "query body"
    }
  }
}
