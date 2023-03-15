/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing.sird

import java.net.URI
import java.net.URL

import play.api.mvc.RequestHeader

class RequiredQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[String] {
  def unapplySeq(qs: QueryString): Option[Seq[String]] = qs.get(paramName).flatMap(_.headOption.map(Seq(_)))
}

class OptionalQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Option[String]] {
  def unapplySeq(qs: QueryString): Option[Seq[Option[String]]] =
    Some(qs.get(paramName).flatMap(_.headOption)).map(Seq(_))
}

class SeqQueryStringParameter(paramName: String) extends QueryStringParameterExtractor[Seq[String]] {
  def unapplySeq(qs: QueryString): Option[Seq[Seq[String]]] = Some(Seq(qs.getOrElse(paramName, Nil)))
}

trait QueryStringParameterExtractor[T] {
  import QueryStringParameterExtractor._
  def unapplySeq(qs: QueryString): Option[Seq[T]]
  def unapplySeq(req: RequestHeader): Option[Seq[T]] = unapplySeq(req.queryString)
  def unapplySeq(uri: URI): Option[Seq[T]]           = unapplySeq(parse(uri.getRawQuery))
  def unapplySeq(uri: URL): Option[Seq[T]]           = unapplySeq(parse(uri.getQuery))
}

object QueryStringParameterExtractor {
  private def parse(query: String): QueryString =
    Option(query).fold(Map.empty[String, Seq[String]]) {
      _.split("&")
        .map {
          _.span(_ != '=') match {
            case (key, v) => key -> v.drop(1) // '=' prefix
          }
        }
        .groupBy(_._1)
        .view
        .mapValues(_.toSeq.map(_._2))
        .toMap
    }

  def required(name: String) = new RequiredQueryStringParameter(name)
  def optional(name: String) = new OptionalQueryStringParameter(name)
  def seq(name: String)      = new SeqQueryStringParameter(name)
}
