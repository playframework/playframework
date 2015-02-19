/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import scala.util.control.NonFatal

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
      case NonFatal(_) => play.api.i18n.Lang.defaultLang
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
          jE.messages.asScala,
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

  implicit def implicitJavaMessages: play.api.i18n.Messages =
    try {
      val jmessages = play.mvc.Http.Context.current().messages()
      play.api.i18n.Messages(jmessages.lang(), jmessages.messagesApi().scalaApi())
    } catch {
      case NonFatal(_) => play.api.i18n.Messages(play.api.i18n.Lang.defaultLang, play.api.i18n.Messages.messagesApiCache(play.api.Play.current))
    }

}
