/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import akka.util.ByteString
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.core.formatters.Multipart

import scala.annotation._

/**
 * Transform a value of type A to a Byte Array.
 *
 * @tparam A the content type
 */
@implicitNotFound("Cannot write an instance of ${A} to HTTP response. Try to define a Writeable[${A}]")
class Writeable[-A](val transform: A => ByteString, val contentType: Option[String]) {
  def toEntity(a: A): HttpEntity      = HttpEntity.Strict(transform(a), contentType)
  def map[B](f: B => A): Writeable[B] = new Writeable(b => transform(f(b)), contentType)
}

/**
 * Helper utilities for `Writeable`.
 */
object Writeable extends DefaultWriteables {
  def apply[A](transform: (A => ByteString), contentType: Option[String]): Writeable[A] =
    new Writeable(transform, contentType)

  /**
   * Creates a `Writeable[A]` using a content type for `A` available in the implicit scope
   * @param transform Serializing function
   */
  def apply[A](transform: A => ByteString)(implicit ct: ContentTypeOf[A]): Writeable[A] =
    new Writeable(transform, ct.mimeType)
}

/**
 * Default Writeable with lower priority.
 */
trait LowPriorityWriteables {

  /**
   * `Writeable` for `play.twirl.api.Content` values.
   */
  implicit def writeableOf_Content[C <: play.twirl.api.Content](
      implicit codec: Codec,
      ct: ContentTypeOf[C]
  ): Writeable[C] = {
    Writeable(content => codec.encode(content.body))
  }
}

/**
 * Default Writeable.
 */
trait DefaultWriteables extends LowPriorityWriteables {

  /**
   * `Writeable` for `play.twirl.api.Xml` values. Trims surrounding whitespace.
   */
  implicit def writeableOf_XmlContent(
      implicit codec: Codec,
      ct: ContentTypeOf[play.twirl.api.Xml]
  ): Writeable[play.twirl.api.Xml] = {
    Writeable(xml => codec.encode(xml.body.trim))
  }

  /**
   * `Writeable` for `NodeSeq` values - literal Scala XML.
   */
  implicit def writeableOf_NodeSeq[C <: scala.xml.NodeSeq](implicit codec: Codec): Writeable[C] = {
    Writeable(xml => codec.encode(xml.toString))
  }

  /**
   * `Writeable` for `NodeBuffer` values - literal Scala XML.
   */
  implicit def writeableOf_NodeBuffer(implicit codec: Codec): Writeable[scala.xml.NodeBuffer] = {
    Writeable(xml => codec.encode(xml.toString))
  }

  /**
   * `Writeable` for `urlEncodedForm` values
   */
  implicit def writeableOf_urlEncodedForm(implicit codec: Codec): Writeable[Map[String, Seq[String]]] = {
    import java.net.URLEncoder
    Writeable(formData =>
      codec.encode(
        formData
          .flatMap { item =>
            item._2.map(c => URLEncoder.encode(item._1, "UTF-8") + "=" + URLEncoder.encode(c, "UTF-8"))
          }
          .mkString("&")
      )
    )
  }

  /**
   * `Writeable` for `JsValue` values that writes to UTF-8, so they can be sent with the application/json media type.
   */
  implicit def writeableOf_JsValue: Writeable[JsValue] = {
    Writeable(a => ByteString.fromArrayUnsafe(Json.toBytes(a)))
  }

  /**
   * `Writeable` for `JsValue` values using an arbitrary codec. Can be used to force a non-UTF-8 encoding for JSON.
   */
  def writeableOf_JsValue(codec: Codec, contentType: Option[String] = None): Writeable[JsValue] = {
    Writeable(a => codec.encode(Json.stringify(a)), contentType)
  }

  /**
   * `Writeable` for `MultipartFormData`.
   *
   * If the passed writeable contains a contentType with a boundary, this boundary will be used to separate the data/file parts of the multipart/form-data body.
   * If you don't pass a contentType with the writeable, or it does not contain a boundary, a random one will be generated.
   */
  @deprecated("Use method that takes boundary and implicit codec", "2.9.0")
  def writeableOf_MultipartFormData[A](
      codec: Codec,
      aWriteable: Writeable[FilePart[A]]
  ): Writeable[MultipartFormData[A]] = writeableOf_MultipartFormData(codec, aWriteable.contentType)

  /**
   * `Writeable` for `MultipartFormData`.
   *
   * If you pass a contentType which contains a boundary, this boundary will be used to separate the data/file parts of the multipart/form-data body.
   * If you don't pass a contentType, or it does not contain a boundary, a random one will be generated.
   */
  @deprecated("Use method that takes boundary and implicit codec", "2.9.0")
  def writeableOf_MultipartFormData[A](
      codec: Codec,
      contentType: Option[String]
  ): Writeable[MultipartFormData[A]] = {
    // If the passed contentType already provides a boundary, we (re)use it for the Content-Disposition header
    val maybeBoundary = for {
      mt         <- contentType.flatMap(MediaType.parse(_))
      (_, value) <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
      boundary   <- value
    } yield boundary
    writeableOf_MultipartFormData(maybeBoundary)(codec)
  }

  /**
   * `Writeable` for `MultipartFormData`.
   *
   * If you pass a boundary, it will be used to separate the data/file parts of the multipart/form-data body.
   * If you don't pass a boundary a random one will be generated.
   */
  def writeableOf_MultipartFormData[A](
      boundary: Option[String]
  )(implicit codec: Codec): Writeable[MultipartFormData[A]] = {
    val resolvedBoundary = boundary.getOrElse(Multipart.randomBoundary())

    def formatDataParts(data: Map[String, Seq[String]]) = {
      val dataParts = data
        .flatMap {
          case (name, values) =>
            values.map { value =>
              s"""--$resolvedBoundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name="${Multipart
                  .escapeParamWithHTML5Strategy(name)}"\r\n\r\n$value\r\n"""
            }
        }
        .mkString("")
      codec.encode(dataParts)
    }

    def filePartHeader(file: FilePart[A]) = {
      val name     = s""""${Multipart.escapeParamWithHTML5Strategy(file.key)}""""
      val filename = s""""${Multipart.escapeParamWithHTML5Strategy(file.filename)}""""
      val contentType = file.contentType
        .map { ct => s"${HeaderNames.CONTENT_TYPE}: $ct\r\n" }
        .getOrElse("")
      codec.encode(
        s"--$resolvedBoundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name; filename=$filename\r\n$contentType\r\n"
      )
    }

    Writeable[MultipartFormData[A]](
      transform = { (form: MultipartFormData[A]) =>
        formatDataParts(form.dataParts) ++ ByteString(form.files.flatMap { file =>
          filePartHeader(file) ++ file.transformRefToBytes() ++ codec.encode("\r\n")
        }: _*) ++ codec.encode(s"--$resolvedBoundary--")
      },
      contentType = Some(s"multipart/form-data; boundary=$resolvedBoundary")
    )
  }

  /**
   * `Writeable` for empty responses.
   */
  implicit val writeableOf_EmptyContent: Writeable[Results.EmptyContent] = new Writeable(_ => ByteString.empty, None)

  /**
   * Straightforward `Writeable` for String values.
   */
  implicit def wString(implicit codec: Codec): Writeable[String] = Writeable[String](str => codec.encode(str))

  /**
   * Straightforward `Writeable` for Array[Byte] values.
   */
  implicit val wByteArray: Writeable[Array[Byte]] = Writeable(bytes => ByteString(bytes))

  /**
   * Straightforward `Writeable` for ByteString values.
   */
  implicit val wBytes: Writeable[ByteString] = Writeable(identity)
}
