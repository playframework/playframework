/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.templates

import play.api.mvc._
import play.templates._
import play.api.http.MimeTypes
import org.apache.commons.lang3.StringEscapeUtils

/**
 * Appendable content using a StringBuilder. Either specify elements or text, not both.
 *
 * Using an Either[TraversableOnce[A], String] impacts performance in an already
 * contentious part of code, so it has been done with both parameters instead.
 *
 * @param elements Sub elements to traverse when creating the resultant string
 * @param text Formatted content
 * @tparam A self-type
 */
abstract class BufferedContent[A <: BufferedContent[A]](protected val elements: TraversableOnce[A], protected val text: String) extends Appendable[A] with Content with play.mvc.Content { this: A =>
  protected def buildString(builder: StringBuilder) {
    if (!elements.isEmpty) {
      elements.foreach { e =>
        e.buildString(builder)
      }
    } else {
      builder.append(text)
    }
  }

  /**
   * This should only ever be called at the top level element
   * to avoid unneeded memory allocation.
   */
  private lazy val builtBody = {
    val builder = new StringBuilder()
    buildString(builder)
    builder.toString
  }

  override def toString = builtBody

  def body = builtBody

}

/**
 * Content type used in default HTML templates.
 */
class Html private (elements: TraversableOnce[Html], text: String) extends BufferedContent[Html](elements, text) {
  def this(text: String) = this(Nil, text)
  def this(elements: TraversableOnce[Html]) = this(elements, "")

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
    new Html(text)
  }
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
    new Html(sb.toString)
  }

  /**
   * Generate an empty HTML fragment
   */
  val empty: Html = new Html("")

  /**
   * Create an HTML Fragment that holds other fragments.
   */
  def fill(elements: TraversableOnce[Html]): Html = new Html(elements)

}

/**
 * Content type used in default text templates.
 */
class Txt private (elements: TraversableOnce[Txt], text: String) extends BufferedContent[Txt](elements, text) {
  def this(text: String) = this(Nil, text)
  def this(elements: TraversableOnce[Txt]) = this(elements, "")

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
    new Txt(text)
  }

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

  /**
   * Generate an empty Txt fragment
   */
  val empty: Txt = new Txt("")

  /**
   * Create an Txt Fragment that holds other fragments.
   */
  def fill(elements: TraversableOnce[Txt]): Txt = new Txt(elements)

}

/**
 * Content type used in default XML templates.
 */
class Xml private (elements: TraversableOnce[Xml], text: String) extends BufferedContent[Xml](elements, text) {
  def this(text: String) = this(Nil, text)
  def this(elements: TraversableOnce[Xml]) = this(elements, "")

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
    new Xml(text)
  }

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

  /**
   * Generate an empty XML fragment
   */
  val empty: Xml = new Xml("")

  /**
   * Create an XML Fragment that holds other fragments.
   */
  def fill(elements: TraversableOnce[Xml]): Xml = new Xml(elements)

}

/**
 * Type used in default JavaScript templates.
 */
class JavaScript private (elements: TraversableOnce[JavaScript], text: String) extends BufferedContent[JavaScript](elements, text) {
  def this(text: String) = this(Nil, text)
  def this(elements: TraversableOnce[JavaScript]) = this(elements, "")

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
  def apply(text: String) = {
    new JavaScript(text)
  }
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

  /**
   * Generate an empty JavaScript fragment
   */
  val empty: JavaScript = new JavaScript("")

  /**
   * Create an JavaScript Fragment that holds other fragments.
   */
  def fill(elements: TraversableOnce[JavaScript]): JavaScript = new JavaScript(elements)

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
