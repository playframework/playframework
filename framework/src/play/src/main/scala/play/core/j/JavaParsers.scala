package play.core.j

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.Files.{ TemporaryFile }

import scala.xml._
import scala.collection.JavaConverters._

/**
 * provides Java centric BodyParsers
 */
object JavaParsers extends BodyParsers {

  import play.mvc.Http.{ RequestBody }

  case class DefaultRequestBody(
      urlFormEncoded: Option[Map[String, Seq[String]]] = None,
      raw: Option[RawBuffer] = None,
      text: Option[String] = None,
      json: Option[JsValue] = None,
      xml: Option[NodeSeq] = None,
      multipart: Option[MultipartFormData[TemporaryFile]] = None,
      override val isMaxSizeExceeded: Boolean = false) extends RequestBody {

    override lazy val asFormUrlEncoded = {
      urlFormEncoded.map(_.mapValues(_.toArray).asJava).orNull
    }

    override def asRaw = {
      raw.map { rawBuffer =>
        new play.mvc.Http.RawBuffer {
          def size = rawBuffer.size
          def asBytes(maxLength: Int) = rawBuffer.asBytes(maxLength).orNull
          def asBytes = rawBuffer.asBytes().orNull
          def asFile = rawBuffer.asFile
          override def toString = rawBuffer.toString
        }
      }.orNull
    }

    override def asText = {
      text.orNull
    }

    override lazy val asJson = {
      import org.codehaus.jackson._
      import org.codehaus.jackson.map._

      json.map { json =>
        new ObjectMapper().readValue(json.toString, classOf[JsonNode])
      }.orNull
    }

    override lazy val asXml = {
      xml.map { xml =>
        play.libs.XML.fromString(xml.toString)
      }.orNull
    }

    override lazy val asMultipartFormData = {
      multipart.map { multipart =>

        new play.mvc.Http.MultipartFormData {

          lazy val asFormUrlEncoded = {
            multipart.asFormUrlEncoded.mapValues(_.toArray).asJava
          }

          lazy val getFiles = {
            multipart.files.map { file =>
              new play.mvc.Http.MultipartFormData.FilePart(
                file.key, file.filename, file.contentType.orNull, file.ref.file)
            }.asJava
          }

        }

      }.orNull
    }

  }

  def anyContent(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.anyContent).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { anyContent =>
        DefaultRequestBody(
          anyContent.asFormUrlEncoded,
          anyContent.asRaw,
          anyContent.asText,
          anyContent.asJson,
          anyContent.asXml,
          anyContent.asMultipartFormData)
      }.fold(identity, identity)
  }

  def json(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.json).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { json =>
        DefaultRequestBody(json = Some(json))
      }.fold(identity, identity)
  }

  def tolerantJson(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.tolerantJson).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { json =>
        DefaultRequestBody(json = Some(json))
      }.fold(identity, identity)
  }

  def xml(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.xml).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { xml =>
        DefaultRequestBody(xml = Some(xml))
      }.fold(identity, identity)
  }

  def tolerantXml(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.tolerantXml).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { xml =>
        DefaultRequestBody(xml = Some(xml))
      }.fold(identity, identity)
  }

  def text(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.text).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { text =>
        DefaultRequestBody(text = Some(text))
      }.fold(identity, identity)
  }

  def tolerantText(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.tolerantText).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { text =>
        DefaultRequestBody(text = Some(text))
      }.fold(identity, identity)
  }

  def formUrlEncoded(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.urlFormEncoded).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { urlFormEncoded =>
        DefaultRequestBody(urlFormEncoded = Some(urlFormEncoded))
      }.fold(identity, identity)
  }

  def multipartFormData(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.multipartFormData).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { multipart =>
        DefaultRequestBody(multipart = Some(multipart))
      }.fold(identity, identity)
  }

  def raw(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(maxLength, parse.raw).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { raw =>
        DefaultRequestBody(raw = Some(raw))
      }.fold(identity, identity)
  }

}
