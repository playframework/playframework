/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import play.core.formatters.Multipart

/**
 * JSON, XML and Multipart Form Data Writables used for Play-WS bodies.
 */
trait WSBodyWritables extends DefaultBodyWritables with JsonBodyWritables with XMLBodyWritables {

  implicit val bodyWritableOf_Multipart: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, _]], _]] = {
    val boundary = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)
  }

}

object WSBodyWritables extends WSBodyWritables
