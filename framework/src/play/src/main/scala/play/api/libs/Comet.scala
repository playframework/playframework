/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.iteratee._
import play.twirl.api._

import play.core.Execution.Implicits.internalContext

/**
 * Helper function to produce a Comet Enumeratee.
 *
 * Example:
 * {{{
 * val cometStream = Enumerator("A", "B", "C") &> Comet(callback = "console.log")
 * }}}
 *
 */
object Comet {

  /**
   * Typeclass for Comet message. Transform each value to a JavaScript message.
   */
  case class CometMessage[A](toJavascriptMessage: A => String)

  /**
   * Default typeclasses for CometMessage.
   */
  object CometMessage {

    /**
     * String messages.
     */
    implicit val stringMessages = CometMessage[String](str => "'" + StringEscapeUtils.escapeEcmaScript(str) + "'")

    /**
     * Json messages.
     */
    implicit val jsonMessages = CometMessage[play.api.libs.json.JsValue](Json.stringify)

  }

  /**
   * Create a Comet Enumeratee.
   *
   * @tparam E Type of messages handled by this comet stream.
   * @param callback Javascript function to call on the browser for each message.
   * @param initialChunk Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data)
   */
  def apply[E](callback: String, initialChunk: Html = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>"))(implicit encoder: CometMessage[E]) = new Enumeratee[E, Html] {

    def applyOn[A](inner: Iteratee[Html, A]): Iteratee[E, Iteratee[Html, A]] = {

      val fedWithInitialChunk = Iteratee.flatten(Enumerator(initialChunk) |>> inner)
      val eToScript = Enumeratee.map[E](data => Html("""<script type="text/javascript">""" + callback + """(""" + encoder.toJavascriptMessage(data) + """);</script>"""))
      eToScript.applyOn(fedWithInitialChunk)
    }
  }
}
