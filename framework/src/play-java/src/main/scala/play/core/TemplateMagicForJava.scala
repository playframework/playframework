/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.Optional

import play.mvc.Http

import scala.annotation.implicitNotFound
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
  // implicit def javaRequestHeader2ScalaRequestHeader(implicit r: Http.RequestHeader): play.api.mvc.RequestHeader = {
  implicit def javaRequest2ScalaRequest(implicit r: Http.Request): play.api.mvc.Request[_] = {
    r.asScala()
  }

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  implicit def implicitJavaMessages: play.api.i18n.MessagesProvider = {
    ctx.messages().asScala
  }

  @implicitNotFound("No Http.Request implicit parameter found when accessing session. You must add it as a template parameter like @(arg1, arg2,...)(implicit request: Http.Request).")
  implicit def request2Session(implicit request: Http.Request): Http.Session = request.session()

  @implicitNotFound("No Http.Request implicit parameter found when accessing flash. You must add it as a template parameter like @(arg1, arg2,...)(implicit request: Http.Request).")
  implicit def request2Flash(implicit request: Http.Request): Http.Flash = request.flash()

  // TODO: Uncomment when the implicitJavaLang method above gets removed (methods interfere)
  //@implicitNotFound("No play.api.i18n.MessagesProvider implicit parameter found when accessing lang. You must add it as a template parameter like @(arg1, arg2,...)(implicit messages: play.i18n.Messages).")
  //implicit def messagesProvider2Lang(implicit msg: play.api.i18n.MessagesProvider): play.api.i18n.Lang = msg.messages.lang

}
