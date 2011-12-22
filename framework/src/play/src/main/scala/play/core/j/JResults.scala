package play.core.j

import play.api.mvc._
import play.api.libs.iteratee._

import scala.collection.JavaConverters._

object JResults extends Results {
  def writeContent(codec: Codec): Writeable[Content] = writeableOf_Content[Content](codec)
  def writeString(codec: Codec): Writeable[String] = Writeable.wString(codec)
  def writeBytes: Writeable[Array[Byte]] = Writeable.wBytes
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfString(codec: Codec): ContentTypeOf[String] = contentTypeOf_String(codec)
  def contentTypeOf(mimeType: String): ContentTypeOf[Content] = ContentTypeOf(Option(mimeType))
  def contentTypeOfEmptyContent: ContentTypeOf[Results.EmptyContent] = contentTypeOf_EmptyContent
  def noContentType[A]: ContentTypeOf[A] = ContentTypeOf(None)
  def contentTypeOfBytes: ContentTypeOf[Array[Byte]] = ContentTypeOf(Some("application/octet-stream"))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def async(p: play.api.libs.concurrent.Promise[Result]) = AsyncResult(p)
  def chunked[A] = new play.api.libs.iteratee.CallbackEnumerator[A]
}

object JResultExtractor {
  
  def getStatus(result: play.mvc.Result): Int = result.getWrappedResult match {
    case Result(status, _) => status
    case r => sys.error("Cannot extract the Status code from a result of type " + r.getClass.getName)
  }
  
  def getHeaders(result: play.mvc.Result): java.util.Map[String,String] = result.getWrappedResult match {
    case Result(_, headers) => headers.asJava
    case r => sys.error("Cannot extract the Status code from a result of type " + r.getClass.getName)
  }
  
  def getBody(result: play.mvc.Result): Array[Byte] = result.getWrappedResult match {
    case r @ SimpleResult(_, bodyEnumerator) => {
      var readAsBytes = Enumeratee.map[r.BODY_CONTENT](r.writeable.transform(_)).transform(Iteratee.consume[Array[Byte]]())
      bodyEnumerator(readAsBytes).flatMap(_.run).value.get
    }
    case r => sys.error("Cannot extract the body content from a result of type " + r.getClass.getName)
  }
  
}