package play.core.parsers

/**
 * converts a URL encoded form(represented as a String) into a Map
 */
object FormUrlEncodedParser {

  def parse(data: String, encoding: String = "utf-8"): Map[String, Seq[String]] = {

    import java.net._
    import scala.collection.mutable.{ HashMap }

    var params = HashMap.empty[String, Seq[String]]

    data.split('&').foreach { param =>

      if (param.contains('=')) {

        val parts = param.split('=')
        val key = URLDecoder.decode(parts.head, encoding)
        val value = URLDecoder.decode(parts.tail.headOption.getOrElse(""), encoding)

        params += key -> (params.get(key).getOrElse(Seq.empty) :+ value)

      }
    }

    params.toMap
  }

}