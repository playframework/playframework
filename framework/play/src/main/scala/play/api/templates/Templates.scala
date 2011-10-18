package play.api.templates

import play.api.mvc._
import play.templates._

case class Html(text: String) extends Appendable[Html] with Content {
  val buffer = new StringBuilder(text)

  def +(other: Html) = {
    buffer.append(other.buffer)
    this
  }
  override def toString = buffer.toString

  def contentType = "text/html"
  def body = toString

}

object Html {
  def empty = Html("")
}

object HtmlFormat extends Format[Html] {
  def raw(text: String) = Html(text)
  def escape(text: String) = Html(org.apache.commons.lang.StringEscapeUtils.escapeHtml(text))
}

object PlayMagic {

  import scala.collection.JavaConverters._

  implicit def javaOptionToScala[T](x: play.libs.F.Option[T]): Option[T] = x match {
    case x: play.libs.F.Some[T] => Some(x.get)
    case x: play.libs.F.None[T] => None
  }

  def toHtmlArgs(args: Seq[(Symbol, Any)]) = Html(args.map(a => a._1.name + "=\"" + a._2 + "\"").mkString(" "))

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