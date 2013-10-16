/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.parsers

/**
 * converts a URL encoded form(represented as a String) into a Map
 */
object FormUrlEncodedParser {

  def parse(data: String, encoding: String = "utf-8"): Map[String, Seq[String]] = {

    import java.net._

    data.split("&").flatMap { param =>
      if (param.contains("=") && !param.startsWith("=")) {
        val parts = param.split("=")
        val key = URLDecoder.decode(parts.head, encoding)
        val value = URLDecoder.decode(parts.tail.headOption.getOrElse(""), encoding)
        Seq(key -> value)
      } else {
        Nil
      }
    }.toSeq.groupBy(_._1).map(param => param._1 -> param._2.map(_._2)).toMap
  }

}