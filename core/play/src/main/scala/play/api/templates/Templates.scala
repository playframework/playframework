/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.templates

import java.util.Optional

import play.twirl.api.Html

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/** Defines a magic helper for Play templates. */
object PlayMagic {

  /**
   * Generates a set of valid HTML attributes.
   *
   * For example:
   * {{{
   * toHtmlArgs(Seq(Symbol("id") -> "item", Symbol("style") -> "color:red"))
   * }}}
   */
  def toHtmlArgs(args: Map[Symbol, Any]) =
    Html(
      args
        .map({
          case (s, None) => s.name
          case (s, v)    => s.name + "=\"" + play.twirl.api.HtmlFormat.escape(v.toString).body + "\""
        })
        .mkString(" ")
    )

  /**
   * Uses the passed MessagesProvider to translates the given argument.
   * If the argument is a raw html, it will translate its string representation and will then again return raw html.
   * The argument to translate can also be a sequence that wraps a string or raw html. In this case every element
   * of the sequence will be translated.
   */
  def translate(arg: Any)(implicit p: play.api.i18n.MessagesProvider): Any = arg match {
    case key: String       => p.messages(key)
    case key: Html         => Html(p.messages(key.toString))
    case Some(key: String) => Some(p.messages(key))
    case Some(key: Html)   => Some(Html(p.messages(key.toString)))
    case key: Optional[_] =>
      key.asScala match {
        case Some(key: String) => Some(p.messages(key)).asJava
        case Some(key: Html)   => Some(Html(p.messages(key.toString))).asJava
        case _                 => arg
      }
    case keys: Seq[_]            => keys.map(key => translate(key))
    case keys: java.util.List[_] => keys.asScala.map(key => translate(key)).asJava
    case keys: Array[_]          => keys.map(key => translate(key))
    case _                       => arg
  }
}
