package play.api.mvc

import play.core._
import play.core.Iteratee._

import play.api.http.Status._
import play.api.http.HeaderNames._

case class SimpleHttpResponse(status:Int, headers:Map[String,String] = Map.empty)

sealed trait Result

// add lenses and pattern matching
case class SimpleResult[A](response:SimpleHttpResponse,body:Enumerator[A])(implicit val writeable:Writeable[A]) extends Result {
    type E=A
}

case class ChunkedResult[A](response:SimpleHttpResponse, chunks:Enumerator[A])(implicit val writeable:Writeable[A]) extends Result {
    type E=A
}

case class SocketResult[A](f: (Enumerator[String], Iteratee[A,Unit]) => Unit)(implicit val writeable: AsString[A]) extends Result

object SocketResult{
      def using[A]( readIn:Iteratee[String,Unit], writeOut: Enumerator[A])(implicit writeable: AsString[A]) =  new SocketResult[A]((e,i) => { readIn <<: e ; i <<: writeOut} )
}

case class AsyncResult(result:Promise[Result]) extends Result

sealed trait Writeable[A]

case class AsString[A](transform:(A => String)) extends Writeable[A]

object AsString {

    implicit val asS_String: AsString[String] =  AsString[String](identity)

}

case class AsBytes[A](transform:(A => Array[Byte])) extends Writeable[A]

object Writeable {
    implicit val wString : Writeable[String] = AsString[String](identity)
    implicit val wBytes : Writeable[Array[Byte]] = AsBytes[Array[Byte]](identity)
}

object Results extends Results

object JResults extends Results {
    def writeContent:Writeable[play.api.Content] = wContent[play.api.Content]
    def writeString:Writeable[String] = Writeable.wString
}

trait Results {
    
    import play.core._
    import play.core.Iteratee._
    
    import play.api._
    import play.api.http.Status._
    import play.api.http.HeaderNames._
    
    implicit def wContent[C <: Content]:Writeable[C] = AsString[C](c => c.body) 
    
    def Status(status:Int, content:String, mimeType:String = "text/html") = SimpleResult(response = SimpleHttpResponse(status, Map(CONTENT_TYPE -> mimeType)), body = Enumerator(content))
    def EmptyStatus(status:Int) = SimpleResult[String](response = SimpleHttpResponse(status), body = Enumerator.empty[String])
    
    class Status(status:Int) extends SimpleResult[String](response = SimpleHttpResponse(status), body = Enumerator.empty[String]) {
        
        def apply[C](content:C, contentType:String = "text/html")(implicit writeable:Writeable[C]):SimpleResult[C] = {
            SimpleResult(response = SimpleHttpResponse(status, Map(CONTENT_TYPE -> contentType)), Enumerator(content))
        }
        
        def apply[C <: Content](content:C)(implicit writeable:Writeable[C]):SimpleResult[C] = apply(content, content.contentType)
        
    }
    
    val Ok = new Status(OK)
    val NotFound = new Status(NOT_FOUND)
    val Forbidden = new Status(FORBIDDEN)
    val BadRequest = new Status(BAD_REQUEST)
    val InternalServerError = new Status(INTERNAL_SERVER_ERROR)
    val NotImplemented = new Status(NOT_IMPLEMENTED)
    
    def Redirect(url:String):SimpleResult[String] = { val r = EmptyStatus(FOUND); r.copy(r.response.copy(headers = Map(LOCATION -> url))) }
    def Redirect(call:Call):SimpleResult[String]= Redirect(call.url)
    
    def Binary(stream:java.io.InputStream, length:Option[Long] = None, contentType:String = "application/octet-stream") = {
        import scalax.io.Resource
        val e = Enumerator(Resource.fromInputStream(stream).byteArray)

        SimpleResult[Array[Byte]](response = SimpleHttpResponse(
            OK, 
            Map(CONTENT_TYPE -> contentType) ++ length.map( length =>
                Map(CONTENT_LENGTH -> (length.toString))).getOrElse(Map.empty) 
            ), 
            body = e
        ) 
        
    }
    
}