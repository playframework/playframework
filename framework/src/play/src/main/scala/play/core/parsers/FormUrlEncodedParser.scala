/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.parsers

import scala.collection.immutable.ListMap

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
   * @param data
   * @param encoding
   * @return
   */
  def parseAsJava(data: String, encoding: String): java.util.Map[String, java.util.List[String]] = {
    import scala.collection.JavaConverters._
    parse(data, encoding).map {
      case (key, values) =>
        key -> values.asJava
    }.asJava
  }

  /**
   * Do the basic parsing into a sequence of key/value pairs
   * @param data The data to parse
   * @param encoding The encoding to use for interpreting the data
   * @return The sequence of key/value pairs
   */
  private def parseToPairs(data: String, encoding: String): Seq[(String, String)] = {

    import java.net._

    // Generate all the pairs, with potentially redundant key values, by parsing the body content.
    data.split('&').flatMap { param =>
      if (param.contains("=") && !param.startsWith("=")) {
        val parts = param.split("=")
        val key = URLDecoder.decode(parts.head, encoding)
        val value = URLDecoder.decode(parts.tail.headOption.getOrElse(""), encoding)
        Seq(key -> value)
      } else {
        Nil
      }
    }.toSeq
  }
}
