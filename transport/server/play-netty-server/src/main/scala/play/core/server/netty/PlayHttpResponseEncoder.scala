/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion

private[server] final class HeadHttpResponse(protocolVersion: HttpVersion, status: HttpResponseStatus)
    extends DefaultFullHttpResponse(protocolVersion, status, Unpooled.EMPTY_BUFFER)

private[server] final class PlayHttpResponseEncoder extends HttpResponseEncoder {
  protected override def isContentAlwaysEmpty(response: HttpResponse): Boolean =
    response.isInstanceOf[HeadHttpResponse] || super.isContentAlwaysEmpty(response)

  protected override def sanitizeHeadersBeforeEncode(response: HttpResponse, isAlwaysEmpty: Boolean): Unit = {
    super.sanitizeHeadersBeforeEncode(response, isAlwaysEmpty)
    if (response.isInstanceOf[HeadHttpResponse]) {
      // HttpStreamsServerHandler needs a synthetic chunked marker when no length is known. Strip both that marker
      // and any application-supplied Transfer-Encoding before writing a HEAD response; a declared Content-Length
      // remains useful response metadata.
      response.headers().remove(HttpHeaderNames.TRANSFER_ENCODING)
      if (response.status().code() == HttpResponseStatus.RESET_CONTENT.code()) {
        // Netty adds Content-Length: 0 while sanitizing 205 responses, but Pekko HTTP omits it for HEAD.
        response.headers().remove(HttpHeaderNames.CONTENT_LENGTH)
      }
    }
  }
}
