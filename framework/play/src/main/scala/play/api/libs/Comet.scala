package play.api.libs

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.templates._

import org.apache.commons.lang.{ StringEscapeUtils }

object Comet {

  case class CometMessage[A](toJavascriptMessage: A => String)

  implicit val stringMessages = CometMessage[String](str => "'" + StringEscapeUtils.escapeJavaScript(str) + "'")

  def apply[A](chunks: Enumerator[A], callback: String, initialChunk: Html = Html(Array.fill[Char](5000)(' ').mkString + "<html><body>"))(implicit encoder: CometMessage[A]): Enumerator[Html] = {
    Enumerator(initialChunk).andThen(chunks.map { input =>
      input.map { data =>
        Html("""<script type="text/javascript">""" + callback + """(""" + encoder.toJavascriptMessage(data) + """)</script>""")
      }
    })
  }

}