/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.mvc.MultipartFormData
import play.core.formatters.Multipart

/**
 * JSON, XML and Multipart Form Data Writables used for Play-WS bodies.
 */
trait WSBodyWritables extends DefaultBodyWritables with JsonBodyWritables with XMLBodyWritables {
  implicit val bodyWritableOf_Multipart: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, ?]], ?]] = {
    val boundary    = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)
  }
}

object WSBodyWritables extends WSBodyWritables
