package play.api.templates

import play.api.mvc._
import play.templates._

/** Content type used in default HTML templates.
  *
  * @param text the HTML text
  */
case class Html(text: String) extends Appendable[Html] with Content {
  val buffer = new StringBuilder(text)

  /** Appends this HTML fragment to another. */
  def +(other: Html) = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  /** Content type of HTML (`text/html`). */
  def contentType = "text/html"

  def body = toString

}

/** Helper for HTML utility methods.
 */
object Html {

  /** Creates an empty HTML fragment. */
  def empty = Html("")

}

/** Formatter for HTML content. */
object HtmlFormat extends Format[Html] {

  /** Creates a raw (unescaped) HTML fragment. */
  def raw(text: String) = Html(text)

  /** Creates a safe (escaped) HTML fragment. */
  def escape(text: String) = Html(org.apache.commons.lang.StringEscapeUtils.escapeHtml(text))

}

/** Content type used in default text templates.
  *
  * @param text The plain text.
  */
case class Txt(text: String) extends Appendable[Txt] with Content {
  val buffer = new StringBuilder(text)

  /** Appends this text fragment to another. */
  def +(other: Txt) = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  /** Content type of text (`text/plain`). */
  def contentType = "text/plain"

  def body = toString

}

/** Helper for utilities Txt methods. */
object Txt {

  /** Creates an empty text fragment. */
  def empty = Txt("")

}

/** Formatter for text content. */
object TxtFormat extends Format[Txt] {

  /** Create a text fragment. */
  def raw(text: String) = Txt(text)

  /** No need for a safe (escaped) text fragment. */
  def escape(text: String) = Txt(text)

}

/** Content type used in default XML templates.
  *
  * @param text the plain xml text
  */
case class Xml(text: String) extends Appendable[Xml] with Content {
  val buffer = new StringBuilder(text)

  /** Append this XML fragment to another. */
  def +(other: Xml) = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  /** Content type of XML (`text/xml`). */
  def contentType = "text/xml"

  def body = toString

}

/** Helper for XML utility methods. */
object Xml {

  /** Create an empty XML fragment. */
  def empty = Xml("")

}

/** Formatter for XML content. */
object XmlFormat extends Format[Xml] {

  /** Creates an XML fragment. */
  def raw(text: String) = Xml(text)

  /** Creates an escaped XML fragment. */
  def escape(text: String) = Xml(org.apache.commons.lang.StringEscapeUtils.escapeXml(text))

}

/** Defines a magic helper for Play templates. */
object PlayMagic {

  import scala.collection.JavaConverters._

  /** Transforms a Play Java `Option` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: play.libs.F.Option[T]): Option[T] = x match {
    case x: play.libs.F.Some[T] => Some(x.get)
    case x: play.libs.F.None[T] => None
  }

  /** Generates a set of valid HTML attributes.
    *
    * For example:
    * {{{
    * toHtmlArgs(Seq('id -> "item", 'style -> "color:red"))
    * }}}
    */
  def toHtmlArgs(args: Seq[(Symbol, Any)]) = Html(args.map(a => a._1.name + "=\"" + a._2 + "\"").mkString(" "))

  /** Transforms a Play Java form `Field` to a proper Scala form `Field`. */
  implicit def javaFieldtoScalaField(field: play.data.Form.Field) = {
    play.api.data.Field(
      field.name,
      field.constraints.asScala.map { jT =>
        jT._1 -> jT._2.asScala
      },
      Option(field.format).map(f => f._1 -> f._2.asScala),
      field.errors.asScala.map { jE =>
        play.api.data.FormError(
          jE.key,
          jE.message,
          jE.arguments.asScala)
      },
      Option(field.value))
  }

}
