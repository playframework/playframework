/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import play.api.mvc._
import play.api._
import play.api.http.Status._
import play.api.http.HeaderNames._
import play.api.libs.json._
import play.twirl.api._

import scala.annotation._

/**
 * Defines the default content type for type `A`.
 *
 * @tparam A the content type
 * @param the default content type for `A`, if any
 */
@implicitNotFound(
  "Cannot guess the content type to use for ${A}. Try to define a ContentTypeOf[${A}]"
)
case class ContentTypeOf[-A](mimeType: Option[String])

/**
 * Default Content-Type typeclasses.
 */
object ContentTypeOf extends DefaultContentTypeOfs

/**
 * Contains typeclasses for ContentTypeOf.
 */
trait DefaultContentTypeOfs {

  /**
   * Default content type for `Html` values (`text/html`).
   */
  implicit def contentTypeOf_Html(implicit codec: Codec): ContentTypeOf[Html] = {
    ContentTypeOf[Html](Some(ContentTypes.HTML))
  }

  /**
   * Default content type for `Xml` values (`application/xml`).
   */
  implicit def contentTypeOf_Xml(implicit codec: Codec): ContentTypeOf[Xml] = {
    ContentTypeOf[Xml](Some(ContentTypes.XML))
  }

  /**
   * Default content type for `JsValue` values (`application/json`).
   */
  implicit def contentTypeOf_JsValue(implicit codec: Codec): ContentTypeOf[JsValue] = {
    ContentTypeOf[JsValue](Some(ContentTypes.JSON))
  }

  /**
   * Default content type for `Txt` values (`text/plain`).
   */
  implicit def contentTypeOf_Txt(implicit codec: Codec): ContentTypeOf[Txt] = {
    ContentTypeOf[Txt](Some(ContentTypes.TEXT))
  }

  /**
   * Default content type for `JavaScript` values.
   */
  implicit def contentTypeOf_JavaScript(implicit codec: Codec): ContentTypeOf[JavaScript] =
    ContentTypeOf[JavaScript](Some(ContentTypes.JAVASCRIPT))

  /**
   * Default content type for `String` values (`text/plain`).
   */
  implicit def contentTypeOf_String(implicit codec: Codec): ContentTypeOf[String] = {
    ContentTypeOf[String](Some(ContentTypes.TEXT))
  }

  /**
   * Default content type for `Map[String, Seq[String]]]` values (`application/x-www-form-urlencoded`).
   */
  implicit def contentTypeOf_urlEncodedForm(implicit codec: Codec): ContentTypeOf[Map[String, Seq[String]]] = {
    ContentTypeOf[Map[String, Seq[String]]](Some(ContentTypes.FORM))
  }

  /**
   * Default content type for `NodeSeq` values (`application/xml`).
   */
  implicit def contentTypeOf_NodeSeq[C <: scala.xml.NodeSeq](implicit codec: Codec): ContentTypeOf[C] = {
    ContentTypeOf[C](Some(ContentTypes.XML))
  }

  /**
   * Default content type for `NodeBuffer` values (`application/xml`).
   */
  implicit def contentTypeOf_NodeBuffer(implicit codec: Codec): ContentTypeOf[scala.xml.NodeBuffer] = {
    ContentTypeOf[scala.xml.NodeBuffer](Some(ContentTypes.XML))
  }

  /**
   * Default content type for byte array (application/application/octet-stream).
   */
  implicit def contentTypeOf_ByteArray: ContentTypeOf[Array[Byte]] = ContentTypeOf[Array[Byte]](Some(ContentTypes.BINARY))

  /**
   * Default content type for empty responses (no content type).
   */
  implicit def contentTypeOf_EmptyContent: ContentTypeOf[Results.EmptyContent] = ContentTypeOf[Results.EmptyContent](None)

}
