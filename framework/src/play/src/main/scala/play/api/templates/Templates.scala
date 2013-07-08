package play.api.templates

import play.api.mvc._
import play.templates._
import play.api.http.MimeTypes
import org.apache.commons.lang3.StringEscapeUtils

/**
 * Appendable content using a StringBuilder.
 * @param buffer StringBuilder to use
 * @tparam A self-type
 */
abstract class BufferedContent[A <: BufferedContent[A]](private val buffer: StringBuilder) extends Appendable[A] with Content with play.mvc.Content { this: A =>

  def +=(other: A) = {
    buffer.append(other.buffer)
    this
  }

  override def toString = buffer.toString()

  def body = toString

}

/**
 * Content type used in default HTML templates.
 */
class Html(buffer: StringBuilder) extends BufferedContent[Html](buffer) {
  /**
   * Content type of HTML.
   */
  val contentType = MimeTypes.HTML
}

/**
 * Helper for HTML utility methods.
 */
object Html {

  /**
   * Creates an HTML fragment with initial content specified.
   */
  def apply(text: String): Html = {
    new Html(new StringBuilder(text))
  }

  /**
   * Creates an empty HTML fragment.
   */
  def empty: Html = new Html(new StringBuilder)
}

/**
 * Formatter for HTML content.
 */
object HtmlFormat extends Format[Html] {

  /**
   * Creates a raw (unescaped) HTML fragment.
   */
  def raw(text: String): Html = Html(text)

  /**
   * Creates a safe (escaped) HTML fragment.
   */
  def escape(text: String): Html = {
    // Using our own algorithm here because commons lang escaping wasn't designed for protecting against XSS, and there
    // don't seem to be any other good generic escaping tools out there.
    val sb = new StringBuilder(text.length)
    text.foreach {
      case '<' => sb.append("&lt;")
      case '>' => sb.append("&gt;")
      case '"' => sb.append("&quot;")
      case '\'' => sb.append("&#x27;")
      case '&' => sb.append("&amp;")
      case c => sb += c
    }
    new Html(sb)
  }

}

/**
 * Content type used in default text templates.
 */
class Txt(buffer: StringBuilder) extends BufferedContent[Txt](buffer) {
  /**
   * Content type of text (`text/plain`).
   */
  def contentType = MimeTypes.TEXT
}

/**
 * Helper for utilities Txt methods.
 */
object Txt {

  /**
   * Creates a text fragment with initial content specified.
   */
  def apply(text: String): Txt = {
    new Txt(new StringBuilder(text))
  }

  /**
   * Creates an empty text fragment.
   */
  def empty = new Txt(new StringBuilder)

}

/**
 * Formatter for text content.
 */
object TxtFormat extends Format[Txt] {

  /**
   * Create a text fragment.
   */
  def raw(text: String) = Txt(text)

  /**
   * No need for a safe (escaped) text fragment.
   */
  def escape(text: String) = Txt(text)

}

/**
 * Content type used in default XML templates.
 */
class Xml(buffer: StringBuilder) extends BufferedContent[Xml](buffer) {
  /**
   * Content type of XML (`application/xml`).
   */
  def contentType = MimeTypes.XML
}

/**
 * Helper for XML utility methods.
 */
object Xml {

  /**
   * Creates an XML fragment with initial content specified.
   */
  def apply(text: String): Xml = {
    new Xml(new StringBuilder(text))
  }

  /**
   * Create an empty XML fragment.
   */
  def empty = new Xml(new StringBuilder)

}

/**
 * Formatter for XML content.
 */
object XmlFormat extends Format[Xml] {

  /**
   * Creates an XML fragment.
   */
  def raw(text: String) = Xml(text)

  /**
   * Creates an escaped XML fragment.
   */
  def escape(text: String) = Xml(org.apache.commons.lang3.StringEscapeUtils.escapeXml(text))

}

/**
 * Type used in default JavaScript templates.
 */
class JavaScript(buffer: StringBuilder) extends BufferedContent[JavaScript](buffer) {
  /**
   * Content type of JavaScript
   */
  val contentType = MimeTypes.JAVASCRIPT
}

/**
 * Helper for JavaScript utility methods.
 */
object JavaScript {
  /**
   * Creates a JavaScript fragment with initial content specified
   */
  def apply(content: String) = new JavaScript(new StringBuilder(content))
}

/**
 * Formatter for JavaScript content.
 */
object JavaScriptFormat extends Format[JavaScript] {
  /**
   * Integrate `text` without performing any escaping process.
   * @param text Text to integrate
   */
  def raw(text: String): JavaScript = JavaScript(text)

  /**
   * Escapes `text` using JavaScript String rules.
   * @param text Text to integrate
   */
  def escape(text: String): JavaScript = JavaScript(StringEscapeUtils.escapeEcmaScript(text))

}

/** Defines a magic helper for Play templates. */
object PlayMagic {

  /**
   * Generates a set of valid HTML attributes.
   *
   * For example:
   * {{{
   * toHtmlArgs(Seq('id -> "item", 'style -> "color:red"))
   * }}}
   */
  def toHtmlArgs(args: Map[Symbol, Any]) = Html(args.map({
    case (s, None) => s.name
    case (s, v) => s.name + "=\"" + HtmlFormat.escape(v.toString).body + "\""
  }).mkString(" "))

}
