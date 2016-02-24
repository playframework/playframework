/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.routing.sird

import java.net.{ URI, URL }

import play.api.mvc.RequestHeader

class RequiredQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[String] {
  def unapply(qs: QueryString): Option[String] = qs.get(paramName).flatMap(_.headOption)
}

class OptionalQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Option[String]] {
  def unapply(qs: QueryString): Option[Option[String]] = Some(qs.get(paramName).flatMap(_.headOption))
}

class SeqQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Seq[String]] {
  def unapply(qs: QueryString): Option[Seq[String]] = Some(qs.getOrElse(paramName, Nil))
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

