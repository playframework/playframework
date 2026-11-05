/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import java.nio.charset.StandardCharsets
import java.util.Locale

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.ReferenceCountUtil
import org.specs2.mutable.Specification

class PlayHttpResponseEncoderSpec extends Specification {

  "PlayHttpResponseEncoder" should {
    "preserve a declared Content-Length for a HEAD response" in {
      val response = headResponse()
      HttpUtil.setContentLength(response, 10)

      val wire = encode(response).toLowerCase(Locale.ROOT)

      wire must contain("content-length: 10\r\n")
      wire must not contain "transfer-encoding"
    }

    "remove the internal chunked marker without writing a terminal chunk" in {
      val response = headResponse()
      HttpUtil.setTransferEncodingChunked(response, true)

      val wire = encode(response).toLowerCase(Locale.ROOT)

      wire must not contain "transfer-encoding"
      wire must not contain "\r\n0\r\n\r\n"
      wire must endWith("\r\n\r\n")
    }

    "remove an application-supplied Transfer-Encoding for a HEAD response" in {
      val response = headResponse()
      response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "gzip")

      val wire = encode(response).toLowerCase(Locale.ROOT)

      wire must not contain "transfer-encoding"
    }

    "remove Content-Length after Netty sanitizes a HEAD 205 response" in {
      val response = headResponse(HttpResponseStatus.RESET_CONTENT)
      HttpUtil.setContentLength(response, 10)

      val wire = encode(response).toLowerCase(Locale.ROOT)

      wire must not contain "content-length"
    }

    "retain Netty's Content-Length for a non-HEAD 205 response" in {
      val response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.RESET_CONTENT, Unpooled.EMPTY_BUFFER)

      val wire = encode(response).toLowerCase(Locale.ROOT)

      wire must contain("content-length: 0\r\n")
    }
  }

  private def headResponse(status: HttpResponseStatus = HttpResponseStatus.OK): HeadHttpResponse =
    new HeadHttpResponse(HttpVersion.HTTP_1_1, status)

  private def encode(response: HttpResponse): String = {
    val channel = new EmbeddedChannel(new PlayHttpResponseEncoder())
    val wire    = new StringBuilder
    try {
      channel.writeOutbound(response)
      var message = channel.readOutbound[AnyRef]()
      while (message != null) {
        try {
          message match {
            case buffer: ByteBuf => wire.append(buffer.toString(StandardCharsets.US_ASCII))
            case other           => throw new IllegalStateException(s"Unexpected outbound message: ${other.getClass}")
          }
        } finally {
          ReferenceCountUtil.release(message)
        }
        message = channel.readOutbound[AnyRef]()
      }
      wire.toString
    } finally {
      channel.finishAndReleaseAll()
    }
  }
}
