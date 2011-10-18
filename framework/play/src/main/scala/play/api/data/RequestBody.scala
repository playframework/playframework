package play.api.data

import play.core.Iteratee._
import play.core.Iteratee
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import play.api.mvc.{ BodyParser, AnyContent }

object RequestData {
  def urlEncoded(encoding: String): BodyParser[AnyContent] = {
    val it = Iteratee.fold[Array[Byte], ArrayBuffer[Byte]](ArrayBuffer[Byte]())(_ ++= _)
    import play.core.UrlEncodedParser
    BodyParser(_ =>
      it.mapDone { a =>
        AnyContent(
          UrlEncodedParser.parse(new String(a.toArray /* should give encoding here */ ), encoding)
            .asScala
            .toMap
            .mapValues(_.toSeq))
      })
  }

}

