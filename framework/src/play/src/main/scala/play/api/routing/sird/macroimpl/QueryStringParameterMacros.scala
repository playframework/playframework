/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
// This is in its own package so that the UrlContext.q interpolator in the sird package doesn't make the
// Quasiquote.q interpolator ambiguous.
package play.api.routing.sird.macroimpl

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros

/**
 * The macros are used to parse and validate the query string parameters at compile time.
 *
 * They generate AST that constructs the extractors directly with the parsed parameter name, instead of having to parse
 * the string context parameters at runtime.
 */
private[sird] object QueryStringParameterMacros {
  val paramEquals = "([^&=]+)=".r

  def required(c: Context) = {
    macroImpl(c, "q", "required")
  }

  def optional(c: Context) = {
    macroImpl(c, "q_?", "optional")
  }

  def seq(c: Context) = {
    macroImpl(c, "q_*", "seq")
  }

  def macroImpl(c: Context, name: String, extractorName: String) = {
    import c.universe._

    // Inspect the prefix, this is call that constructs the StringContext, containing the StringContext parts
    c.prefix.tree match {
      case Apply(_, List(Apply(_, rawParts))) =>
        // extract the part literals
        val parts = rawParts map { case Literal(Constant(const: String)) => const }

        // Extract paramName, and validate
        val startOfString = c.enclosingPosition.point + name.length + 1
        val paramName = parts.head match {
          case paramEquals(param) => param
          case _ => c.abort(c.enclosingPosition.withPoint(startOfString), "Invalid start of string for query string extractor '" + parts.head + "', extractor string must have format " + name + "\"param=$extracted\"")
        }

        if (parts.length == 1) {
          c.abort(c.enclosingPosition.withPoint(startOfString + paramName.length), "Unexpected end of String, expected parameter extractor, eg $extracted")
        }

        if (parts.length > 2) {
          c.abort(c.enclosingPosition, "Query string extractor can only extract one parameter, extract multiple parameters using the & extractor, eg: " + name + "\"param1=$param1\" & " + name + "\"param2=$param2\"")
        }

        if (parts(1).nonEmpty) {
          c.abort(c.enclosingPosition, s"Unexpected text at end of query string extractor: '${parts(1)}'")
        }

        // Return AST that invokes the desired method to create the extractor on QueryStringParameterExtractor, passing
        // the parameter name to it
        val call = TermName(extractorName)
        c.Expr(
          q"_root_.play.api.routing.sird.QueryStringParameterExtractor.$call($paramName)"
        )

      case _ =>
        c.abort(c.enclosingPosition, "Invalid use of query string extractor")
    }
  }

}
