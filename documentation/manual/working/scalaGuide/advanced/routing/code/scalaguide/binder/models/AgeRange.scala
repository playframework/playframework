/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
  //#bindQuery
  implicit def queryStringBindable(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[AgeRange] {
    override def bindQuery(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgeRange]] = {
      for {
        from <- intBinder.bindQuery("from", params)
        to   <- intBinder.bindQuery("to", params)
      } yield {
        (from, to) match {
          case (Right(from), Right(to)) => Right(AgeRange(from, to))
          case _                        => Left("Unable to bind an AgeRange")
        }
      }
    }
    override def unbindQuery(key: String, ageRange: AgeRange): String = {
      intBinder.unbindQuery("from", ageRange.from) + "&" + intBinder.unbindQuery("to", ageRange.to)
    }
  }
  //#bindQuery
}
