package play.core.data

import play.core.Iteratee._
import play.core.Iteratee
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

object RequestData {
    def urlEncoded(encoding:String) :Iteratee[Array[Byte],Map[String,Seq[String]]] = {
        val it = Iteratee.fold[Array[Byte],ArrayBuffer[Byte]](ArrayBuffer[Byte]())( _  ++= _)
        import play.data.parsing.UrlEncodedParser
        it.mapDone{a => 
            UrlEncodedParser.parse(new String(a.toArray /* should give encoding here */),encoding)
                            .asScala
                            .toMap
                            .mapValues(_.toSeq)
                 }
    }

}
