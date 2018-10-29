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

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  private def ctx = Http.Context.current()

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  implicit def implicitJavaLang: play.api.i18n.Lang = {
    try {
      ctx.lang
    } catch {
      case NonFatal(_) => play.api.i18n.Lang.defaultLang
    }
  }

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  implicit def requestHeader: play.api.mvc.RequestHeader = {
    ctx.request().asScala
  }

  // TODO: After removing Http.Context (and the corresponding methods in this object here) this should be changed to:
  // implicit def javaRequestHeader2ScalaRequestHeader(implicit r: play.mvc.Http.RequestHeader): play.api.mvc.RequestHeader = {
  implicit def javaRequest2ScalaRequest(implicit r: play.mvc.Http.Request): play.api.mvc.Request[_] = {
    r.asScala()
  }

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  implicit def implicitJavaMessages: play.api.i18n.MessagesProvider = {
    ctx.messages().asScala
  }

}
