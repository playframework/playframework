/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.Optional

import play.mvc.Http

import scala.annotation.implicitNotFound
import scala.collection.convert.ToJavaImplicits
import scala.collection.convert.ToScalaImplicits

/** Defines a magic helper for Play templates in a Java context. */
object PlayMagicForJava extends ToScalaImplicits with ToJavaImplicits {
  import scala.compat.java8.OptionConverters._
  import scala.language.implicitConversions

  /** Transforms a Play Java `Optional` to a proper Scala `Option`. */
  implicit def javaOptionToScala[T](x: Optional[T]): Option[T] = x.asScala

  @implicitNotFound(
    """An implicit play.mvc.Http.RequestHeader (or play.mvc.Http.Request) is necessary so that it can be converted to a play.api.mvc.RequestHeader.
    You must add it as a template parameter like @(arg1, arg2, ...)(implicit request: play.mvc.Http.RequestHeader)."""
  )
  implicit def javaRequestHeader2ScalaRequestHeader(implicit r: Http.RequestHeader): play.api.mvc.RequestHeader = {
    r.asScala()
  }

  @implicitNotFound(
    "No play.mvc.Http.Request implicit parameter found when accessing session. You must add it as a template parameter like @(arg1, arg2, ...)(implicit request: Http.Request)."
  )
  implicit def request2Session(implicit request: Http.Request): Http.Session = request.session()

  @implicitNotFound(
    "No play.mvc.Http.Request implicit parameter found when accessing flash. You must add it as a template parameter like @(arg1, arg2, ...)(implicit request: Http.Request)."
  )
  implicit def request2Flash(implicit request: Http.Request): Http.Flash = request.flash()

  @implicitNotFound(
    "No play.api.i18n.MessagesProvider implicit parameter found when accessing Lang. You must add it as a template parameter like @(arg1, arg2, ...)(implicit messages: play.i18n.Messages)."
  )
  implicit def messagesProvider2Lang(implicit msg: play.api.i18n.MessagesProvider): play.api.i18n.Lang =
    msg.messages.lang
}
