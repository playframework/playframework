package play.core.parsers

object UrlFormEncodedParser {

  def parse(data: String): Map[String, Seq[String]] = {

    import java.net._
    import scala.collection.mutable.{ HashMap }

    var params = HashMap.empty[String, Seq[String]]

    data.split('&').foreach { param =>

      if (param.contains('=')) {

        val parts = param.split('=')
        val key = parts.head
        val value = URLDecoder.decode(parts.tail.headOption.getOrElse(""), "utf-8")

        params += key -> (params.get(key).getOrElse(Seq.empty) :+ value)

      }
    }

    params.toMap
  }

}