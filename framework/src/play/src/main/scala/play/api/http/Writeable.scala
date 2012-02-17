package play.api.http

import play.api.mvc._
import play.api._
import play.api.http.Status._
import play.api.http.HeaderNames._
import play.api.templates._
import play.api.libs.json._

import scala.annotation._

/**
 * Transform a value of type A to a Byte Array.
 *
 * @tparam A the content type
 */
@implicitNotFound(
  "Cannot write an instance of ${A} to HTTP response. Try to define a Writeable[${A}]"
)
case class Writeable[-A](transform: (A => Array[Byte]))

/**
 * Helper utilities for `Writeable`.
 */
object Writeable extends DefaultWriteables

/**
 * Default Writeable with lowwe priority.
 */
trait LowPriorityWriteables {

  /**
   * `Writeable` for `Content` values.
   */
  implicit def writeableOf_Content[C <: Content](implicit codec: Codec): Writeable[C] = {
    Writeable[C](content => codec.encode(content.body))
  }

}

/**
 * Default Writeable.
 */
trait DefaultWriteables extends LowPriorityWriteables {

  /**
   * `Writeable` for `NodeSeq` values - literal Scala XML.
   */
  implicit def writeableOf_NodeSeq[C <: scala.xml.NodeSeq](implicit codec: Codec): Writeable[C] = {
    Writeable[C](xml => codec.encode(xml.toString))
  }

  /**
   * `Writeable` for `NodeBuffer` values - literal Scala XML.
   */
  implicit def writeableOf_NodeBuffer(implicit codec: Codec): Writeable[scala.xml.NodeBuffer] = {
    Writeable[scala.xml.NodeBuffer](xml => codec.encode(xml.toString))
  }

  /**
   * `Writeable` for `urlEncodedForm` values
   */
  implicit def writeableOf_urlEncodedForm(implicit codec: Codec): Writeable[Map[String, Seq[String]]] = {
    Writeable[Map[String, Seq[String]]](formData => codec.encode(formData.map(item => item._2.map(c => item._1 + "=" + c)).flatten.mkString("&")))
  }

  /**
   * `Writeable` for `JsValue` values - Json
   */
  implicit def writeableOf_JsValue(implicit codec: Codec): Writeable[JsValue] = {
    Writeable[JsValue](jsval => codec.encode(jsval.toString))
  }

  /**
   * `Writeable` for empty responses.
   */
  implicit val writeableOf_EmptyContent = Writeable[Results.EmptyContent](_ => Array.empty)

  /**
   * Straightforward `Writeable` for String values.
   */
  implicit def wString(implicit codec: Codec): Writeable[String] = Writeable[String](str => codec.encode(str))

  /**
   * Straightforward `Writeable` for Array[Byte] values.
   */
  implicit val wBytes: Writeable[Array[Byte]] = Writeable[Array[Byte]](identity)

}

