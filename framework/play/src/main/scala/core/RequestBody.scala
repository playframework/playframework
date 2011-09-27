package play.core.data

import play.core.Iteratee._
import play.core.Iteratee
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import play.api.mvc.BodyParser

object RequestData {
    def urlEncoded(encoding:String) :BodyParser[Map[String,Seq[String]]] = {
        val it = Iteratee.fold[Array[Byte],ArrayBuffer[Byte]](ArrayBuffer[Byte]())( _  ++= _)
        import play.data.parsing.UrlEncodedParser
        BodyParser( _ =>
        it.mapDone{a => 
            UrlEncodedParser.parse(new String(a.toArray /* should give encoding here */),encoding)
                            .asScala
                            .toMap
                            .mapValues(_.toSeq)
                 })
    }

}
