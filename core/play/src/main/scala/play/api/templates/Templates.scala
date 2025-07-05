/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.templates

import java.util.Optional

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import play.twirl.api.Html

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
        .map {
          case (s, None) => s.name
          case (s, v)    => s.name + "=\"" + play.twirl.api.HtmlFormat.escape(v.toString).body + "\""
        }
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
    case key: Optional[?]  =>
      key.toScala match {
        case Some(key: String) => Some(p.messages(key)).toJava
        case Some(key: Html)   => Some(Html(p.messages(key.toString))).toJava
        case _                 => arg
      }
    case keys: Seq[?]            => keys.map(key => translate(key))
    case keys: java.util.List[?] => keys.asScala.map(key => translate(key)).asJava
    case keys: Array[?]          => keys.map(key => translate(key))
    case _                       => arg
  }
}
