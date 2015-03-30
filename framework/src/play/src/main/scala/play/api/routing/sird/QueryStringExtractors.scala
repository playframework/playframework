/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import java.net.{ URI, URL }

import play.api.mvc.RequestHeader

import scala.reflect.macros.Context

class RequiredQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[String] {
  def unapply(qs: QueryString): Option[String] = qs.get(paramName).flatMap(_.headOption)
}

class OptionalQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Option[String]] {
  def unapply(qs: QueryString): Option[Option[String]] = Some(qs.get(paramName).flatMap(_.headOption))
}

class SeqQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Seq[String]] {
  def unapply(qs: QueryString): Option[Seq[String]] = Some(qs.get(paramName).getOrElse(Nil))
}

trait QueryStringParameterExtractor[T] {
  import QueryStringParameterExtractor._
  def unapply(qs: QueryString): Option[T]
  def unapply(req: RequestHeader): Option[T] = unapply(req.queryString)
  def unapply(uri: URI): Option[T] = unapply(parse(uri.getRawQuery))
  def unapply(uri: URL): Option[T] = unapply(parse(uri.getQuery))
}

object QueryStringParameterExtractor {
  private def parse(query: String): QueryString = {
    Option(query).map(_.split("&").toSeq.map { keyValue =>
      keyValue.split("=", 2) match {
        case Array(key, value) => key -> value
        case Array(key) => key -> ""
      }
    }.groupBy(_._1).mapValues(_.map(_._2)).toMap)
      .getOrElse(Map.empty)
  }

  def required(name: String) = new RequiredQueryStringParameter(name)
  def optional(name: String) = new OptionalQueryStringParameter(name)
  def seq(name: String) = new SeqQueryStringParameter(name)
}

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

        // the QueryStringParameterExtractor object
        val sirdPackage = Select(Select(Select(Ident(newTermName("play")), newTermName("api")),
          newTermName("routing")), newTermName("sird"))
        val extractor = Select(sirdPackage, newTermName("QueryStringParameterExtractor"))

        // Return AST that invokes the desired method to create the extractor on QueryStringParameterExtractor, passing
        // the parameter name to it
        c.Expr(Apply(
          Select(extractor, newTermName(extractorName)),
          List(Literal(Constant(paramName)))
        ))

      case _ =>
        c.abort(c.enclosingPosition, "Invalid use of query string extractor")
    }
  }

}