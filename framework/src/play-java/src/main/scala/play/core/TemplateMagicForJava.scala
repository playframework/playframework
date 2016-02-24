/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.util.Optional
import scala.util.control.NonFatal

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava {

  import scala.collection.JavaConverters._
  import scala.language.implicitConversions
  import scala.compat.java8.OptionConverters._

  /** Transforms a Play Java `Optional` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: Optional[T]): Option[T] = x.asScala

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
      case NonFatal(_) =>
        val app = play.api.Play.privateMaybeApplication.get
        val api = play.api.i18n.Messages.messagesApiCache(app)
        val lang = play.api.i18n.Lang.defaultLang
        play.api.i18n.Messages(lang, api)
    }

}
