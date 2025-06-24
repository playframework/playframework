/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// This is in its own package so that the UrlContext.q interpolator in the sird package doesn't make the
// Quasiquote.q interpolator ambiguous.
package play.api.routing.sird.macroimpl

import scala.quoted.*

import play.api.routing.sird.QueryStringParameterExtractor

/**
 * The macros are used to parse and validate the query string parameters at compile time.
 *
 * They generate AST that constructs the extractors directly with the parsed parameter name, instead of having to parse
 * the string context parameters at runtime.
 */
private[sird] object QueryStringParameterMacros {
  val paramEquals = "([^&=]+)=".r

  def required(clz: Expr[StringContext])(using q: Quotes) = {
    macroImpl(clz, "q", e => '{ QueryStringParameterExtractor.required(${ e }) })
  }

  def optional(clz: Expr[StringContext])(using q: Quotes) = {
    macroImpl(clz, "q_?", e => '{ QueryStringParameterExtractor.optional(${ e }) })
  }

  def seq(clz: Expr[StringContext])(using q: Quotes) = {
    macroImpl(clz, "q_*", e => '{ QueryStringParameterExtractor.seq(${ e }) })
  }

  def macroImpl[E](sc: Expr[StringContext], name: String, fn: Expr[String] => Expr[E])(using q: Quotes): Expr[E] = {
    import q.reflect.*

    // scala3 version of scala2 `scala.reflect.api.Position.withPoint`
    def withPoint(pos: Position, start: Int): Position = {
      Position(pos.sourceFile, start, start)
    }

    sc match {
      case '{ StringContext(${ Varargs(rawParts) }*) } =>
        val parts: Seq[String] = Expr.ofSeq(rawParts).valueOrAbort

        if (parts.sizeIs <= 0) {
          report.errorAndAbort(
            "Invalid use of query string extractor with empty parts"
          )
        }

        if (parts.sizeIs > 2) {
          report.errorAndAbort(
            "Query string extractor can only extract one parameter, extract multiple parameters using the & extractor, eg: " + name + "\"param1=$param1\" & " + name + "\"param2=$param2\""
          )
        }

        // Extract paramName, and validate
        val startOfString = Position.ofMacroExpansion.start + name.length + 1
        val paramName     = parts.head match {
          case paramEquals(param) => param
          case _                  =>
            report.errorAndAbort(
              "Invalid start of string for query string extractor '" + parts.head + "', extractor string must have format " + name + "\"param=$extracted\"",
              withPoint(Position.ofMacroExpansion, startOfString)
            )
        }

        if (parts.sizeIs == 1) {
          report.errorAndAbort(
            "Unexpected end of String, expected parameter extractor, eg $extracted",
            withPoint(Position.ofMacroExpansion, startOfString + paramName.length)
          )
        }

        // Because of the above validation we know for sure now that parts has a length of 2
        if (parts(1).nonEmpty) {
          report.errorAndAbort(s"Unexpected text at end of query string extractor: '${parts(1)}'")
        }

        fn(Expr(paramName))
      case _ =>
        report.errorAndAbort(
          "Invalid use of query string extractor"
        )
    }

  }
}
