package play.core.j

import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._

import scala.collection.JavaConverters._
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie }

/**
 * Java compatible Results
 */
object JavaResults extends Results with DefaultWriteables with DefaultContentTypeOfs {
  def writeContent(codec: Codec): Writeable[Content] = writeableOf_Content[Content](codec)
  def writeString(codec: Codec): Writeable[String] = Writeable.wString(codec)
  def writeBytes: Writeable[Array[Byte]] = Writeable.wBytes
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfString(codec: Codec): ContentTypeOf[String] = contentTypeOf_String(codec)
  def contentTypeOfHtml(implicit codec: Codec): ContentTypeOf[String] = ContentTypeOf(Some(play.api.http.ContentTypes.HTML))
  def contentTypeOfJson(implicit codec: Codec): ContentTypeOf[String] = ContentTypeOf(Some(play.api.http.ContentTypes.JSON))
  def contentTypeOf(mimeType: String): ContentTypeOf[Content] = ContentTypeOf(Option(mimeType))
  def contentTypeOfEmptyContent: ContentTypeOf[Results.EmptyContent] = contentTypeOf_EmptyContent
  def noContentType[A]: ContentTypeOf[A] = ContentTypeOf(None)
  def contentTypeOfBytes: ContentTypeOf[Array[Byte]] = ContentTypeOf(Some("application/octet-stream"))
  def contentTypeOfBytes(mimeType: String): ContentTypeOf[Array[Byte]] = ContentTypeOf(Option(mimeType).orElse(Some("application/octet-stream")))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def async(p: play.api.libs.concurrent.Promise[Result]) = AsyncResult(p)
  def chunked[A](onDisconnected: () => Unit) = play.api.libs.iteratee.Enumerator.imperative[A](onComplete = onDisconnected)
  def chunked(stream: java.io.InputStream, chunkSize: Int) = Enumerator.fromStream(stream, chunkSize)
  def chunked(file: java.io.File, chunkSize: Int) = Enumerator.fromFile(file, chunkSize)
}
import play.api.libs.concurrent._
object JavaResultExtractor {

  def getStatus(result: play.mvc.Result): Int = result.getWrappedResult match {
    case r: AsyncResult => getStatus(new ResultWrapper(r.result.await.get))
    case Result(status, _) => status
    case r => sys.error("Cannot extract the Status code from a result of type " + r.getClass.getName)
  }

  def getCookies(result: play.mvc.Result): JCookies = result.getWrappedResult match {
    case r: AsyncResult => getCookies(new ResultWrapper(r.result.await.get))
    case Result(_, headers) => new JCookies {
      def get(name: String) = {
        Cookies(headers.get(HeaderNames.SET_COOKIE)).get(name).map { cookie =>
          new JCookie(cookie.name, cookie.value, cookie.maxAge, cookie.path, cookie.domain.getOrElse(null), cookie.secure, cookie.httpOnly)
        }.getOrElse(null)
      }
    }
    case r => sys.error("Cannot extract Headers from a result of type " + r.getClass.getName)
  }

  def getHeaders(result: play.mvc.Result): java.util.Map[String, String] = result.getWrappedResult match {
    case r: AsyncResult => getHeaders(new ResultWrapper(r.result.await.get))
    case Result(_, headers) => headers.asJava
    case r => sys.error("Cannot extract the Status code from a result of type " + r.getClass.getName)
  }

  def getBody(result: play.mvc.Result): Array[Byte] = result.getWrappedResult match {
    case r: AsyncResult => getBody(new ResultWrapper(r.result.await.get))
    case r @ SimpleResult(_, bodyEnumerator) => {
      var readAsBytes = Enumeratee.map[r.BODY_CONTENT](r.writeable.transform(_)).transform(Iteratee.consume[Array[Byte]]())
      bodyEnumerator(readAsBytes).flatMap(_.run).value1.get
    }
    case r => sys.error("Cannot extract the body content from a result of type " + r.getClass.getName)
  }

  class ResultWrapper(r: play.api.mvc.Result) extends play.mvc.Result {
    def getWrappedResult = r
  }

}
