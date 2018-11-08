/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.Optional

import play.mvc.Http

import scala.annotation.implicitNotFound

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava extends JavaImplicitConversions {

  import scala.language.implicitConversions
  import scala.compat.java8.OptionConverters._

  /** Transforms a Play Java `Optional` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: Optional[T]): Option[T] = x.asScala

  implicit def javaRequestHeader2ScalaRequestHeader(implicit r: Http.RequestHeader): play.api.mvc.RequestHeader = r.asScala()

  @implicitNotFound("No Http.Request implicit parameter found when accessing session. You must add it as a template parameter like @(implicit request: Http.Request).")
  def session(implicit request: Http.Request): Http.Session = request.session()

  @implicitNotFound("No Http.Request implicit parameter found when accessing flash. You must add it as a template parameter like @(implicit request: Http.Request).")
  def flash(implicit request: Http.Request): Http.Flash = request.flash()

  @implicitNotFound("No play.api.i18n.MessagesProvider implicit parameter found when accessing lang. You must add it as a template parameter like @(implicit messages: play.i18n.Messages).")
  def lang(implicit msg: play.api.i18n.MessagesProvider): play.api.i18n.Lang = msg.messages.lang

}
