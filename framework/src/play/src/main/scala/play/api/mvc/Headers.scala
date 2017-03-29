/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.util.Locale

import play.core.utils.CaseInsensitiveOrdered
import play.mvc.Http.HeaderNames

import scala.collection.immutable.{ TreeMap, TreeSet }

/**
 * The HTTP headers set.
 *
 * @param _headers The sequence of values. This value is protected and mutable
 * since subclasses might initially set it to a `null` value and then initialize
 * it lazily.
 */
class Headers(protected var headers: Seq[(String, String)]) {

  // TODO expose the actual Long content length? esp.since available in Akka HTTP backend easily
  //  /** INTERNAL API: Content-Length or -1 if not known. */
  //  private[play] def contentLength: Long =
  //    get(HeaderNames.CONTENT_LENGTH).map(_.toLong).getOrElse(-1)

  /**
   * The headers as a sequence of name-value pairs.
   */
  def headers: Seq[(String, String)] = _headers

  /**
   * Append the given headers
   */
  def add(headers: (String, String)*) = new Headers(this.headers ++ headers)

  /**
   * Retrieves the first header value which is associated with the given key.
   */
  def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))

  override def equals(other: Any) = {
    other.isInstanceOf[Headers] &&
      toMap == other.asInstanceOf[Headers].toMap
  }

  /**
   * Optionally returns the first header value associated with a key.
   */
  def get(key: String): Option[String] = getAll(key).headOption

  /**
   * Retrieve all header values associated with the given key.
   */
  def getAll(key: String): Seq[String] = toMap.getOrElse(key, Nil)

  override def hashCode = {
    toMap.map {
      case (name, value) =>
        name.toLowerCase(Locale.ENGLISH) -> value
    }.hashCode()
  }

  /**
   * Retrieve all header keys
   */
  def keys: Set[String] = toMap.keySet

  /**
   * Remove any headers with the given keys
   */
  def remove(keys: String*) = {
    val keySet = TreeSet(keys: _*)(CaseInsensitiveOrdered)
    new Headers(headers.filterNot { case (name, _) => keySet(name) })
  }

  /**
   * Append the given headers, replacing any existing headers having the same keys
   */
  def replace(headers: (String, String)*) = remove(headers.map(_._1): _*).add(headers: _*)

  /**
   * Transform the Headers to a Map
   */
  lazy val toMap: Map[String, Seq[String]] = {
    val map = headers.groupBy(_._1.toLowerCase(Locale.ENGLISH)).map {
      case (_, headers) =>
        // choose the case of first header as canonical
        headers.head._1 -> headers.map(_._2)
    }
    TreeMap(map.toSeq: _*)(CaseInsensitiveOrdered)
  }

  /**
   * Transform the Headers to a Map by ignoring multiple values.
   */
  lazy val toSimpleMap: Map[String, String] = toMap.mapValues(_.headOption.getOrElse(""))

  override def toString = headers.toString()

}

object Headers {

  /**
   * For calling from Java.
   */
  def create() = new Headers(Seq.empty)

  def apply(headers: (String, String)*) = new Headers(headers)

}
