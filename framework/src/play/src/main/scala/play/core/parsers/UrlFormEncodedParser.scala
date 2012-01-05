package play.core.parsers

object UrlFormEncodedParser {

  def parse(data: String): Map[String, Seq[String]] = {

    import java.net._

    data.split('&').foldLeft(Map.empty[String, Seq[String]]) { case (params, param) =>

      if (param.contains('=')) {

        val parts = param.split('=').map(URLDecoder.decode(_, "utf-8"))
        val key = parts.head
        val value = parts.tail.headOption.getOrElse("")

        params + (key -> (params.get(key).getOrElse(Seq.empty) :+ value))

      } else params
    }
  }

}