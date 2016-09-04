/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.http.scaladsl.coding.{ DataMapper, Decoder, Deflate, Gzip }
import akka.http.scaladsl.model.HttpRequest

object HttpRequestDecoder {

  def decodeRequestWith(decoder: Decoder, request: HttpRequest): HttpRequest = {
    decoder.decode(request)(DataMapper.mapRequest)
  }

  def decodeRequest(request: HttpRequest): HttpRequest = {
    request.encoding match {
      case Gzip.encoding => decodeRequestWith(Gzip, request)
      case Deflate.encoding => decodeRequestWith(Deflate, request)
      // Handle every undefined decoding as is
      case _ => request
    }
  }

}
