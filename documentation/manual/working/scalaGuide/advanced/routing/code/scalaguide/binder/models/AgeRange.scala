/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.binder.models

import scala.Left
import scala.Right
import play.api.mvc.PathBindable
import play.Logger
import play.api.mvc.QueryStringBindable

//#declaration
case class AgeRange(from: Int, to: Int) {}
//#declaration
object AgeRange {

  //#bind
  implicit def queryStringBindable(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[AgeRange] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgeRange]] = {
      for {
        from <- intBinder.bind("from", params)
        to   <- intBinder.bind("to", params)
      } yield {
        (from, to) match {
          case (Right(from), Right(to)) => Right(AgeRange(from, to))
          case _                        => Left("Unable to bind an AgeRange")
        }
      }
    }
    override def unbind(key: String, ageRange: AgeRange): String = {
      intBinder.unbind("from", ageRange.from) + "&" + intBinder.unbind("to", ageRange.to)
    }
  }
  //#bind
}
