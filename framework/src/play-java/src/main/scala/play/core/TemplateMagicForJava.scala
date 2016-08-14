/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.util.Optional
import scala.util.control.NonFatal

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava {

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

  implicit def requestHeader: play.api.mvc.RequestHeader = {
    play.mvc.Http.Context.Implicit.ctx._requestHeader
  }

  implicit def implicitJavaMessages: play.api.i18n.Messages =
    try {
      val context = play.mvc.Http.Context.current()
      context.messages().asScala
    } catch {
      case NonFatal(_) =>
        val app = play.api.Play.privateMaybeApplication.get
        val api = play.api.i18n.Messages.messagesApiCache(app)
        val lang = play.api.i18n.Lang.defaultLang
        play.api.i18n.MessagesImpl(lang, api)
    }

}
