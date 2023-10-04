/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.pekkohttp

import org.apache.pekko.http.scaladsl.model.headers.`Content-Encoding`
import org.apache.pekko.http.scaladsl.model.headers.HttpEncodings
import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.stream.scaladsl.Compression
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import org.apache.pekko.NotUsed

/**
 * Utilities for decoding a request whose body has been encoded, i.e.
 * `Content-Encoding` is set.
 */
private[server] object HttpRequestDecoder {

  /**
   * Decode the request with a decoder. Remove the `Content-Encoding` header
   * since the body will no longer be encoded.
   */
  private def decodeRequestWith(
      decoderFlow: Flow[ByteString, ByteString, NotUsed],
      request: HttpRequest
  ): HttpRequest = {
    request
      .withEntity(request.entity.transformDataBytes(decoderFlow))
      .withHeaders(request.headers.filterNot(_.isInstanceOf[`Content-Encoding`]))
  }

  /**
   * Decode the request body if it is encoded and we know how to decode it.
   */
  def decodeRequest(request: HttpRequest): HttpRequest = {
    request.encoding match {
      case HttpEncodings.gzip    => decodeRequestWith(Compression.gunzip(), request)
      case HttpEncodings.deflate => decodeRequestWith(Compression.inflate(), request)
      // Handle every undefined decoding as is
      case _ => request
    }
  }
}
