/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.parsers

import java.net.URLDecoder

/** An object for parsing application/x-www-form-urlencoded data */
object FormUrlEncodedParser {

  /**
   * Parse the content type "application/x-www-form-urlencoded" which consists of a bunch of & separated key=value
   * pairs, both of which are URL encoded.
   * @param data The body content of the request, or whatever needs to be so parsed
   * @param encoding The character encoding of data
   * @return A ListMap of keys to the sequence of values for that key
   */
  def parseNotPreservingOrder(data: String, encoding: String = "utf-8"): Map[String, Seq[String]] = {
    // Generate the pairs of values from the string.
    parseToPairs(data, encoding).groupBy(_._1).map(param => param._1 -> param._2.map(_._2)).toMap
  }

  /**
   * Parse the content type "application/x-www-form-urlencoded" which consists of a bunch of & separated key=value
   * pairs, both of which are URL encoded. We are careful in this parser to maintain the original order of the
   * keys by using OrderPreserving.groupBy as some applications depend on the original browser ordering.
   * @param data The body content of the request, or whatever needs to be so parsed
   * @param encoding The character encoding of data
   * @return A ListMap of keys to the sequence of values for that key
   */
  def parse(data: String, encoding: String = "utf-8"): Map[String, Seq[String]] = {

    // Generate the pairs of values from the string.
    val pairs: Seq[(String, String)] = parseToPairs(data, encoding)

    // Group the pairs by the key (first item of the pair) being sure to preserve insertion order
    play.utils.OrderPreserving.groupBy(pairs)(_._1)
  }

  /**
   * Parse the content type "application/x-www-form-urlencoded", mapping to a Java compatible format.
   * @param data The body content of the request, or whatever needs to be so parsed
   * @param encoding The character encoding of data
   * @return A Map of keys to the sequence of values for that key
   */
  def parseAsJava(data: String, encoding: String): java.util.Map[String, java.util.List[String]] = {
    import scala.collection.JavaConverters._
    parse(data, encoding).map {
      case (key, values) =>
        key -> values.asJava
    }.asJava
  }

  /**
   * Parse the content type "application/x-www-form-urlencoded", mapping to a Java compatible format.
   * @param data The body content of the request, or whatever needs to be so parsed
   * @param encoding The character encoding of data
   * @return A Map of keys to the sequence of array values for that key
   */
  def parseAsJavaArrayValues(data: String, encoding: String): java.util.Map[String, Array[String]] = {
    import scala.collection.JavaConverters._
    parse(data, encoding).map {
      case (key, values) =>
        key -> values.toArray
    }.asJava
  }

  private[this] val parameterDelimiter = "[&;]".r

  /**
   * Do the basic parsing into a sequence of key/value pairs
   * @param data The data to parse
   * @param encoding The encoding to use for interpreting the data
   * @return The sequence of key/value pairs
   */
  private def parseToPairs(data: String, encoding: String): Seq[(String, String)] = {
    parameterDelimiter.split(data).map { param =>
      val parts = param.split("=", -1)
      val key = URLDecoder.decode(parts(0), encoding)
      val value = URLDecoder.decode(parts.lift(1).getOrElse(""), encoding)
      key -> value
    }
  }
}
