package play.core.j

import play.api.mvc._
import play.templates._

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava {

  import scala.collection.JavaConverters._
  import scala.language.implicitConversions

  /** Transforms a Play Java `Option` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: play.libs.F.Option[T]): Option[T] = x match {
    case x: play.libs.F.Some[_] => Some(x.get)
    case x: play.libs.F.None[_] => None
  }

  implicit def implicitJavaLang: play.api.i18n.Lang = {
    try {
      play.mvc.Http.Context.Implicit.lang.asInstanceOf[play.api.i18n.Lang]
    } catch {
      case _: Throwable => play.api.i18n.Lang.defaultLang
    }
  }

  /**
   * Implicit conversion of a Play Java form `Field` to a proper Scala form `Field`.
   */
  implicit def javaFieldtoScalaField(jField: play.data.Form.Field): play.api.data.Field = {

    new play.api.data.Field(
      null,
      jField.name,
      jField.constraints.asScala.map { jT =>
        jT._1 -> jT._2.asScala
      },
      Option(jField.format).map(f => f._1 -> f._2.asScala),
      jField.errors.asScala.map { jE =>
        play.api.data.FormError(
          jE.key,
          jE.message,
          jE.arguments.asScala)
      },
      Option(jField.value)) {

      override def apply(key: String) = {
        javaFieldtoScalaField(jField.sub(key))
      }

      override lazy val indexes = jField.indexes.asScala.toSeq.map(_.toInt)

    }
  }

  implicit def requestHeader: play.api.mvc.RequestHeader = {
    play.mvc.Http.Context.Implicit.ctx._requestHeader
  }

}
