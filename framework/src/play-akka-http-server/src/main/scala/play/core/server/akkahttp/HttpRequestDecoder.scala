/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.NotUsed
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{ HttpEncodings, `Content-Encoding` }
import akka.stream.scaladsl.{ Compression, Flow }
import akka.util.ByteString

private[server] object HttpRequestDecoder {

  private def decodeRequestWith(decoderFlow: Flow[ByteString, ByteString, NotUsed], request: HttpRequest): HttpRequest = {
    request.withEntity(request.entity.transformDataBytes(decoderFlow))
      .withHeaders(request.headers.filter(_.isInstanceOf[`Content-Encoding`]))
  }

  def decodeRequest(request: HttpRequest): HttpRequest = {
    request.encoding match {
      case HttpEncodings.gzip => decodeRequestWith(Compression.gunzip(), request)
      case HttpEncodings.deflate => decodeRequestWith(Compression.inflate(), request)
      // Handle every undefined decoding as is
      case _ => request
    }
  }

}
