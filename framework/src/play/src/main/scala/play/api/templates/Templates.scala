/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.templates

/** Defines a magic helper for Play templates. */
object PlayMagic {

  /**
   * Generates a set of valid HTML attributes.
   *
   * For example:
   * {{{
   * toHtmlArgs(Seq('id -> "item", 'style -> "color:red"))
   * }}}
   */
  def toHtmlArgs(args: Map[Symbol, Any]) = play.twirl.api.Html(args.map({
    case (s, None) => s.name
    case (s, v) => s.name + "=\"" + play.twirl.api.HtmlFormat.escape(v.toString).body + "\""
  }).mkString(" "))

}
