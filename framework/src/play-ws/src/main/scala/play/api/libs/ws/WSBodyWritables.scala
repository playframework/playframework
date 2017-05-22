package play.api.libs.ws

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import play.core.formatters.Multipart

trait WSBodyWritables {

  implicit val bodyWritableOf_Multipart: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, _]], _]] = {
    val boundary = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    BodyWritable(b => StreamedBody(Multipart.transform(b, boundary)), contentType)
  }

}

object WSBodyWritables extends WSBodyWritables
