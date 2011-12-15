package play.api.libs

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.templates._

import org.apache.commons.lang.{ StringEscapeUtils }

object Comet {

  case class CometMessage[A](toJavascriptMessage: A => String)

  implicit val stringMessages = CometMessage[String](str => "'" + StringEscapeUtils.escapeJavaScript(str) + "'")

  def apply[E](callback: String, initialChunk: Html = Html(Array.fill[Char](5000)(' ').mkString + "<html><body>"))(implicit encoder: CometMessage[E]) = new Enumeratee[E, Html] {

    def apply[A](inner: Iteratee[Html, A]): Iteratee[E, Iteratee[Html, A]] = {

      val fedWithInitialChunk = Iteratee.flatten(inner <<: Enumerator(initialChunk))
      val eToScript = Enumeratee.map[E](data => Html("""<script type="text/javascript">""" + callback + """(""" + encoder.toJavascriptMessage(data) + """);</script>"""))
      eToScript(fedWithInitialChunk)
    }
  }
}
