/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.{ ByteString, ByteStringBuilder }
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json.{ JsValue, Json }
import play.twirl.api._

/**
 * Helper function to produce a Comet using <a href="http://doc.akka.io/docs/akka/2.5/scala/stream/index.html">Akka Streams</a>.
 *
 * Please see <a href="https://en.wikipedia.org/wiki/Comet_(programming)">https://en.wikipedia.org/wiki/Comet_(programming)</a>
 * for details of Comet.
 *
 * Example:
 *
 * {{{
 *   def streamClock() = Action {
 *     val df: DateTimeFormatter = DateTimeFormatter.ofPattern("HH mm ss")
 *     val tickSource = Source.tick(0 millis, 100 millis, "TICK")
 *     val source = tickSource.map((tick) => df.format(ZonedDateTime.now()))
 *     Ok.chunked(source via Comet.flow("parent.clockChanged"))
 *   }
 * }}}
 *
 */
object Comet {

  val initialHtmlChunk = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>")

  val initialByteString = ByteString.fromString(initialHtmlChunk.toString())

  /**
   * Produces a Flow of escaped ByteString from a series of String elements.  Calls
   * out to Comet.flow internally.
   *
   * @param callbackName the javascript callback method.
   * @return a flow of ByteString elements.
   */
  def string(callbackName: String): Flow[String, ByteString, NotUsed] = {
    Flow[String].map(str =>
      ByteString.fromString("'" + StringEscapeUtils.escapeEcmaScript(str) + "'")
    ).via(flow(callbackName))
  }

  /**
   * Produces a flow of ByteString using `Json.fromJson(_).get` from a Flow of JsValue.  Calls
   * out to Comet.flow internally.
   *
   * @param callbackName the javascript callback method.
   * @return a flow of ByteString elements.
   */
  def json(callbackName: String): Flow[JsValue, ByteString, NotUsed] = {
    Flow[JsValue].map { msg =>
      ByteString.fromString(Json.asciiStringify(msg))
    }.via(flow(callbackName))
  }

  /**
   * Creates a flow of ByteString.  Useful when you have objects that are not JSON or String where
   * you may have to do your own conversion.
   *
   * Usage example:
   *
   * {{{
   *   val htmlStream: Source[ByteString, ByteString, NotUsed] = Flow[Html].map { html =>
   *     ByteString.fromString(html.toString())
   *   }
   *   ...
   *   Ok.chunked(htmlStream via Comet.flow("parent.clockChanged"))
   * }}}
   */
  def flow(callbackName: String, initialChunk: ByteString = initialByteString): Flow[ByteString, ByteString, NotUsed] = {
    val cb: ByteString = ByteString.fromString(callbackName)
    Flow.apply[ByteString].map(msg => formatted(cb, msg)).prepend(Source.single(initialChunk))
  }

  private def formatted(callbackName: ByteString, javascriptMessage: ByteString): ByteString = {
    val b: ByteStringBuilder = new ByteStringBuilder
    b.append(ByteString.fromString("""<script type="text/javascript">"""))
    b.append(callbackName)
    b.append(ByteString.fromString("("))
    b.append(javascriptMessage)
    b.append(ByteString.fromString(");</script>"))
    b.result
  }

  private def toHtml[A](callbackName: String, message: A): Html = {
    val javascriptMessage = message match {
      case str: String =>
        "'" + StringEscapeUtils.escapeEcmaScript(str) + "'"
      case json: JsValue =>
        Json.stringify(json)
      case other =>
        throw new IllegalStateException("Illegal type found: only String or JsValue elements are valid")
    }
    Html(s"""<script type="text/javascript">${callbackName}(${javascriptMessage});</script>""")
  }

}
