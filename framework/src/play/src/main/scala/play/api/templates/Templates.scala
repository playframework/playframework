package play.api.templates

import play.api.mvc._
import play.templates._

/**
 * Content type used in default HTML templates.
 *
 * @param text the HTML text
 */
case class Html(text: String) extends Appendable[Html] with Content with play.mvc.Content {
  val buffer = new StringBuilder(text)

  /**
   * Appends this HTML fragment to another.
   */
  def +(other: Html): Html = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  /**
   * Content type of HTML (`text/html`).
   */
  def contentType: String = "text/html"

  def body: String = toString

}

/**
 * Helper for HTML utility methods.
 */
object Html {

  /**
   * Creates an empty HTML fragment.
   */
  def empty: Html = Html("")

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
  def escape(text: String): Html = Html(org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(text))

}

/**
 * Content type used in default text templates.
 *
 * @param text The plain text.
 */
case class Txt(text: String) extends Appendable[Txt] with Content with play.mvc.Content {
  val buffer = new StringBuilder(text)

  /**
   * Appends this text fragment to another.
   */
  def +(other: Txt): this.type = {
    buffer.append(other.buffer)
    this
  }

  override def toString = buffer.toString

  /**
   * Content type of text (`text/plain`).
   */
  def contentType = "text/plain"

  def body = toString

}

/**
 * Helper for utilities Txt methods.
 */
object Txt {

  /**
   * Creates an empty text fragment.
   */
  def empty = Txt("")

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
 *
 * @param text the plain xml text
 */
case class Xml(text: String) extends Appendable[Xml] with Content with play.mvc.Content {
  val buffer = new StringBuilder(text)

  /** Append this XML fragment to another. */
  def +(other: Xml) = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  /**
   * Content type of XML (`text/xml`).
   */
  def contentType = "text/xml"

  def body = toString

}

/**
 * Helper for XML utility methods.
 */
object Xml {

  /**
   * Create an empty XML fragment.
   */
  def empty = Xml("")

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
  def toHtmlArgs(args: Map[Symbol, Any]) = Html(args.map(a => a._1.name + "=\"" + a._2 + "\"").mkString(" "))

}
