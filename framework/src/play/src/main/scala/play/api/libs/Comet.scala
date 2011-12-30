package play.api.libs

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.templates._

import org.apache.commons.lang.{ StringEscapeUtils }

object Comet {

  case class CometMessage[A](toJavascriptMessage: A => String)

  object CometMessage {
    implicit val stringMessages = CometMessage[String](str => "'" + StringEscapeUtils.escapeJavaScript(str) + "'")
    implicit val jsonMessages = CometMessage[play.api.libs.json.JsValue](json => json.toString())
  }

  def apply[E](callback: String, initialChunk: Html = Html(Array.fill[Char](5000)(' ').mkString + "<html><body>"))(implicit encoder: CometMessage[E]) = new Enumeratee[E, Html] {

    def applyOn[A](inner: Iteratee[Html, A]): Iteratee[E, Iteratee[Html, A]] = {

      val fedWithInitialChunk = Iteratee.flatten(Enumerator(initialChunk) |>> inner)
      val eToScript = Enumeratee.map[E](data => Html("""<script type="text/javascript">""" + callback + """(""" + encoder.toJavascriptMessage(data) + """);</script>"""))
      eToScript.applyOn(fedWithInitialChunk)
    }
  }
}
