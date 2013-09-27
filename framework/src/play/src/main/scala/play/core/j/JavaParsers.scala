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
      json.map { json =>
        play.libs.Json.parse(json.toString)
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

  def anyContent(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.anyContent).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      anyContent =>
        DefaultRequestBody(
          anyContent.asFormUrlEncoded,
          anyContent.asRaw,
          anyContent.asText,
          anyContent.asJson,
          anyContent.asXml,
          anyContent.asMultipartFormData)
    )
  }

  def json(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.json(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      json =>
        DefaultRequestBody(json = Some(json))
    )
  }

  def tolerantJson(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.tolerantJson(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      json =>
        DefaultRequestBody(json = Some(json))
    )
  }

  def xml(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.xml(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      xml =>
        DefaultRequestBody(xml = Some(xml))
    )
  }

  def tolerantXml(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.tolerantXml(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      xml =>
        DefaultRequestBody(xml = Some(xml))
    )
  }

  def text(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.text(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      text =>
        DefaultRequestBody(text = Some(text))
    )
  }

  def tolerantText(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.tolerantText(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      text =>
        DefaultRequestBody(text = Some(text))
    )
  }

  def formUrlEncoded(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.urlFormEncoded(Integer.MAX_VALUE)).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      urlFormEncoded =>
        DefaultRequestBody(urlFormEncoded = Some(urlFormEncoded))
    )
  }

  def multipartFormData(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.multipartFormData).map {
    _.fold(
      _ => DefaultRequestBody(isMaxSizeExceeded = true),
      multipart =>
        DefaultRequestBody(multipart = Some(multipart))
    )
  }

  def raw(maxLength: Int): BodyParser[RequestBody] = parse.maxLength(orDefault(maxLength), parse.raw).map { body =>
    body
      .left.map(_ => DefaultRequestBody(isMaxSizeExceeded = true))
      .right.map { raw =>
        DefaultRequestBody(raw = Some(raw))
      }.fold(identity, identity)
  }

  private def orDefault(maxLength: Int) = if (maxLength < 0) BodyParsers.parse.DEFAULT_MAX_TEXT_LENGTH else maxLength

}
