/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import play.core.parsers.Multipart

import scala.collection.JavaConverters._
import scala.xml._

import com.fasterxml.jackson.databind.JsonNode

import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Execution.trampoline
import play.api.libs.json.Reads.JsonNodeReads
import play.api.libs.json._
import play.api.mvc._

/**
 * provides Java centric BodyParsers
 */
object JavaParsers extends BodyParsers {

  import play.mvc.Http.RequestBody

  case class DefaultRequestBody(
      urlFormEncoded: Option[Map[String, Seq[String]]] = None,
      raw: Option[RawBuffer] = None,
      text: Option[String] = None,
      json: Option[JsValue] = None,
      xml: Option[NodeSeq] = None,
      multipart: Option[MultipartFormData[TemporaryFile]] = None) extends RequestBody {

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

    override def asText = text.orNull

    override lazy val asJson = json.map(Json.fromJson[JsonNode](_).get).orNull

    override lazy val asXml = xml.map { xml =>
      play.libs.XML.fromString(xml.toString)
    }.orNull

    override lazy val asMultipartFormData = multipart.map { multipart =>
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

  def default_(maxLength: Long): BodyParser[RequestBody] = anyContent(parse.default(Some(maxLength).filter(_ >= 0)))

  def anyContent(maxLength: Long): BodyParser[RequestBody] = anyContent(parse.anyContent(Some(maxLength).filter(_ >= 0)))

  private def anyContent(parser: BodyParser[AnyContent]): BodyParser[RequestBody] =
    parser.map { anyContent =>
      DefaultRequestBody(
        anyContent.asFormUrlEncoded,
        anyContent.asRaw,
        anyContent.asText,
        anyContent.asJson,
        anyContent.asXml,
        anyContent.asMultipartFormData)
    }(trampoline)

  def json(maxLength: Long): BodyParser[RequestBody] =
    parse.json(orDefault(maxLength)).map { json =>
      DefaultRequestBody(json = Some(json))
    }(trampoline)

  def tolerantJson(maxLength: Long): BodyParser[RequestBody] =
    parse.tolerantJson(orDefault(maxLength)).map { json =>
      DefaultRequestBody(json = Some(json))
    }(trampoline)

  def xml(maxLength: Long): BodyParser[RequestBody] =
    parse.xml(orDefault(maxLength)).map { xml =>
      DefaultRequestBody(xml = Some(xml))
    }(trampoline)

  def tolerantXml(maxLength: Long): BodyParser[RequestBody] =
    parse.tolerantXml(orDefault(maxLength)).map { xml =>
      DefaultRequestBody(xml = Some(xml))
    }(trampoline)

  def text(maxLength: Long): BodyParser[RequestBody] =
    parse.text(orDefault(maxLength)).map { text =>
      DefaultRequestBody(text = Some(text))
    }(trampoline)

  def tolerantText(maxLength: Long): BodyParser[RequestBody] =
    parse.tolerantText(orDefault(maxLength)).map { text =>
      DefaultRequestBody(text = Some(text))
    }(trampoline)

  def formUrlEncoded(maxLength: Long): BodyParser[RequestBody] =
    parse.urlFormEncoded(orDefault(maxLength)).map { urlFormEncoded =>
      DefaultRequestBody(urlFormEncoded = Some(urlFormEncoded))
    }(trampoline)

  def multipartFormData(maxLength: Long): BodyParser[RequestBody] = {
    val maxLengthOrDefault = if (maxLength < 0) BodyParsers.parse.DefaultMaxDiskLength else maxLength
    parse.multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLengthOrDefault).map { multipart =>
      DefaultRequestBody(multipart = Some(multipart))
    }(trampoline)
  }

  def raw(maxLength: Long): BodyParser[RequestBody] =
    parse.raw(parse.DefaultMaxTextLength, orDefault(maxLength)).map { raw =>
      DefaultRequestBody(raw = Some(raw))
    }(trampoline)

  def empty(): BodyParser[RequestBody] = parse.empty.map {
    (_: Unit) => new RequestBody()
  }(trampoline)

  private def orDefault(maxLength: Long) = if (maxLength < 0) parse.DefaultMaxTextLength else maxLength.toInt

}
