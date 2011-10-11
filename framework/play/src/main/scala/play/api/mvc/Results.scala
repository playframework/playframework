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
    
    def withHeaders(headers:(String,String)*) = {
        copy(response = response.copy(headers = response.headers ++ headers))
    }
    
    def withCookies(cookies:Cookie*) = {
        withHeaders(SET_COOKIE -> Cookies.merge(response.headers.get(SET_COOKIE).getOrElse(""), cookies))
    }
    
    def discardingCookies(names:String*) = {
        withHeaders(SET_COOKIE -> Cookies.merge(response.headers.get(SET_COOKIE).getOrElse(""), Nil, discard = names))
    }
    
    def withSession(session:Session):SimpleResult[A] = {
        if(session.isEmpty) discardingCookies(Session.SESSION_COOKIE_NAME) else withCookies(Session.encodeAsCookie(session))
    }
    
    def withSession(session:(String,String)*):SimpleResult[A] = withSession(Session(session.toMap))
    
    def withNewSession = withSession(Session())
    
    def as(contentType:String) = withHeaders(CONTENT_TYPE -> contentType)
    
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
case class AsBytes[A](transform:(A => Array[Byte])) extends Writeable[A]

object Writeable {
    implicit val wString : Writeable[String] = AsString[String](identity)
    implicit val wBytes : Writeable[Array[Byte]] = AsBytes[Array[Byte]](identity)
}

case class ContentTypeOf[A](resolve:(A => Option[String]))



object Results extends Results {
    case class Empty()
}

object JResults extends Results {
    def writeContent:Writeable[play.api.Content] = writeableStringOf_Content[play.api.Content]
    def writeString:Writeable[String] = writeableStringOf_String
    def writeEmpty:Writeable[Results.Empty] = writeableStringOf_Empty
    def contentTypeOfString:ContentTypeOf[String] = contentTypeOf_String
    def contentTypeOfContent:ContentTypeOf[play.api.Content] = contentTypeOf_Content[play.api.Content]
    def contentTypeOfEmpty:ContentTypeOf[Results.Empty] = contentTypeOf_Empty
    def emptyHeaders = Map.empty[String,String]
    def empty = Results.Empty()
}

trait Results {
    
    import play.core._
    import play.core.Iteratee._
    
    import play.api._
    import play.api.http.Status._
    import play.api.http.HeaderNames._
    
    implicit val writeableStringOf_String: AsString[String] = AsString[String](identity)
    implicit def writeableStringOf_Content[C <: Content]:Writeable[C] = AsString[C](c => c.body) 
    implicit def writeableStringOf_NodeSeq[C <: scala.xml.NodeSeq] = AsString[C](x => x.toString)
    implicit val writeableStringOf_Empty = AsString[Results.Empty](_ => "")
    
    implicit val contentTypeOf_String = ContentTypeOf[String](_ => Some("text/plain"))
    implicit def contentTypeOf_Content[C <: Content] = ContentTypeOf[C](c => Some(c.contentType))
    implicit def contentTypeOf_NodeSeq[C <: scala.xml.NodeSeq] = ContentTypeOf[C](_ => Some("text/xml"))
    implicit def contentTypeOf_Empty = ContentTypeOf[Results.Empty](_ => None)
        
        
        
    class Status(status:Int) extends SimpleResult[String](response = SimpleHttpResponse(status), body = Enumerator.empty[String]) {
        
        def apply[C](content:C = Results.Empty(), headers:Map[String,String] = Map.empty)(implicit writeable:Writeable[C], contentTypeOf:ContentTypeOf[C]):SimpleResult[C] = {
            SimpleResult(response = SimpleHttpResponse(status, contentTypeOf.resolve(content).map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty) ++ headers), Enumerator(content))
        }
        
    }
    
    val Ok = new Status(OK)
    val Unauthorized = new Status(UNAUTHORIZED)
    val NotFound = new Status(NOT_FOUND)
    val Forbidden = new Status(FORBIDDEN)
    val BadRequest = new Status(BAD_REQUEST)
    val InternalServerError = new Status(INTERNAL_SERVER_ERROR)
    val NotImplemented = new Status(NOT_IMPLEMENTED)
    def Status(code:Int) = new Status(code)
    
    def Redirect(url:String):SimpleResult[Results.Empty] = Status(FOUND)(headers = Map(LOCATION -> url))
    def Redirect(call:Call):SimpleResult[Results.Empty] = Redirect(call.url)
    
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