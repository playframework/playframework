/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.Optional

import play.mvc.Http

import scala.util.control.NonFatal

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava extends JavaImplicitConversions {

  import scala.language.implicitConversions
  import scala.compat.java8.OptionConverters._

  /** Transforms a Play Java `Optional` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: Optional[T]): Option[T] = x.asScala

  private def ctx = Http.Context.current()

  implicit def implicitJavaLang: play.api.i18n.Lang = {
    try {
      ctx.lang
    } catch {
      case NonFatal(_) => play.api.i18n.Lang.defaultLang
    }
  }

  implicit def requestHeader: play.api.mvc.RequestHeader = {
    ctx._requestHeader
  }

  implicit def implicitJavaMessages: play.api.i18n.Messages = {
    ctx.messages().asScala
  }

}
