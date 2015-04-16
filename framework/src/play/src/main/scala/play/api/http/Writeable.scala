/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee.Enumeratee
import scala.annotation._
import scala.concurrent.ExecutionContext

/**
 * Transform a value of type A to a Byte Array.
 *
 * @tparam A the content type
 */
@implicitNotFound(
  "Cannot write an instance of ${A} to HTTP response. Try to define a Writeable[${A}]"
)
class Writeable[-A](val transform: (A => Array[Byte]), val contentType: Option[String])(implicit ec: ExecutionContext) {
  def map[B](f: B => A): Writeable[B] = Writeable(b => transform(f(b)), contentType)
  def toEnumeratee[E <: A]: Enumeratee[E, Array[Byte]] = Enumeratee.map[E](transform)
}

/**
 * Helper utilities for `Writeable`.
 */
object Writeable extends DefaultWriteables {

  def apply[A](transform: (A => Array[Byte]), contentType: Option[String])(implicit ec: ExecutionContext): Writeable[A] =
    new Writeable(transform, contentType)

  /**
   * Creates a `Writeable[A]` using a content type for `A` available in the implicit scope
   * @param transform Serializing function
   */
  def apply[A](transform: A => Array[Byte])(implicit ct: ContentTypeOf[A], ec: ExecutionContext): Writeable[A] =
    new Writeable(transform, ct.mimeType)

}

/**
 * Default Writeable with lowwe priority.
 */
trait LowPriorityWriteables {

  import play.api.libs.iteratee.Execution.Implicits.trampoline

  /**
   * `Writeable` for `play.twirl.api.Content` values.
   */
  implicit def writeableOf_Content[C <: play.twirl.api.Content](implicit codec: Codec, ct: ContentTypeOf[C]): Writeable[C] = {
    Writeable(content => codec.encode(content.body))
  }

}

/**
 * Default Writeable.
 */
trait DefaultWriteables extends LowPriorityWriteables {

  import play.api.libs.iteratee.Execution.Implicits.trampoline

  /**
   * `Writeable` for `play.twirl.api.Xml` values. Trims surrounding whitespace.
   */
  implicit def writeableOf_XmlContent(implicit codec: Codec, ct: ContentTypeOf[play.twirl.api.Xml]): Writeable[play.twirl.api.Xml] = {
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
      codec.encode(formData.map(item => item._2.map(c => item._1 + "=" + URLEncoder.encode(c, "UTF-8"))).flatten.mkString("&"))
    )
  }

  /**
   * `Writeable` for `JsValue` values - Json
   */
  implicit def writeableOf_JsValue(implicit codec: Codec): Writeable[JsValue] = {
    Writeable(a => codec.encode(Json.stringify(a)))
  }

  /**
   * `Writeable` for empty responses.
   */
  implicit val writeableOf_EmptyContent: Writeable[Results.EmptyContent] = Writeable(_ => Array.empty)

  /**
   * Straightforward `Writeable` for String values.
   */
  implicit def wString(implicit codec: Codec): Writeable[String] = Writeable[String](str => codec.encode(str))

  /**
   * Straightforward `Writeable` for Array[Byte] values.
   */
  implicit val wBytes: Writeable[Array[Byte]] = Writeable(identity)

}

