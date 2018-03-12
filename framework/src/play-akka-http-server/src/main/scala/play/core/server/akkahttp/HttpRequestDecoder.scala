/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.NotUsed
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest }
import akka.http.scaladsl.model.headers.{ HttpEncodings, `Content-Encoding` }
import akka.stream.scaladsl.{ Compression, Flow }
import akka.util.ByteString

/**
 * Utilities for decoding a request whose body has been encoded, i.e.
 * `Content-Encoding` is set.
 */
private[server] object HttpRequestDecoder {

  /**
   * Decode the request with a decoder. Remove the `Content-Encoding` header
   * since the body will no longer be encoded.
   */
  private def decodeRequestWith(decoderFlow: Flow[ByteString, ByteString, NotUsed], request: HttpRequest): HttpRequest = {
    request.withEntity(request.entity.transformDataBytes(decoderFlow))
      .withHeaders(request.headers.filterNot(_.lowercaseName == "content-encoding"))
  }

  /**
   * Decode the request body if it is encoded and we know how to decode it.
   */
  def decodeRequest(request: HttpRequest): HttpRequest = {
    val contentEncoding: Option[HttpHeader] = request.headers.find(_.lowercaseName == `Content-Encoding`.lowercaseName)
    contentEncoding match {
      case None => request
      case Some(ce) =>
        ce.value match {
          case HttpEncodings.gzip.value => decodeRequestWith(Compression.gunzip(), request)
          case HttpEncodings.deflate.value => decodeRequestWith(Compression.inflate(), request)
          case _ => request
        }
    }
  }

}
