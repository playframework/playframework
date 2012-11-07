package play.core.j

import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._

import scala.collection.JavaConverters._
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie, Session => JSession, Flash => JFlash }

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
  def async(p: scala.concurrent.Future[Result]) = AsyncResult(p)
  def chunked[A](onDisconnected: () => Unit) = play.api.libs.iteratee.Enumerator.imperative[A](onComplete = onDisconnected)
  def chunked(stream: java.io.InputStream, chunkSize: Int): Enumerator[Array[Byte]] = Enumerator.fromStream(stream, chunkSize)
  def chunked(file: java.io.File, chunkSize: Int) = Enumerator.fromFile(file, chunkSize)
}
import play.api.libs.concurrent._
object JavaResultExtractor {

  def getStatus(result: play.mvc.Result): Int = result.getWrappedResult match {
    case r: AsyncResult => getStatus(new ResultWrapper(r.result.await.get))
    case PlainResult(status, _) => status
  }

  def getCookies(result: play.mvc.Result): JCookies = result.getWrappedResult match {
    case r: AsyncResult => getCookies(new ResultWrapper(r.result.await.get))
    case PlainResult(_, headers) => new JCookies {
      def get(name: String) = {
        Cookies(headers.get(HeaderNames.SET_COOKIE)).get(name).map { cookie =>
          new JCookie(cookie.name, cookie.value, cookie.maxAge.map(i => new Integer(i)).orNull, cookie.path, cookie.domain.orNull, cookie.secure, cookie.httpOnly)
        }.getOrElse(null)
      }
    }
  }

  def getSession(result: play.mvc.Result): JSession = result.getWrappedResult match {
    case r: AsyncResult => getSession(new ResultWrapper(r.result.await.get))
    case PlainResult(_, headers) => new JSession(Session.decodeFromCookie(
      Cookies(headers.get(HeaderNames.SET_COOKIE)).get(Session.COOKIE_NAME)
    ).data.asJava)
  }

  def getFlash(result: play.mvc.Result): JFlash = result.getWrappedResult match {
    case r: AsyncResult => getFlash(new ResultWrapper(r.result.await.get))
    case PlainResult(_, headers) => new JFlash(Flash.decodeFromCookie(
      Cookies(headers.get(HeaderNames.SET_COOKIE)).get(Flash.COOKIE_NAME)
    ).data.asJava)
  }

  def getHeaders(result: play.mvc.Result): java.util.Map[String, String] = result.getWrappedResult match {
    case r: AsyncResult => getHeaders(new ResultWrapper(r.result.await.get))
    case PlainResult(_, headers) => headers.asJava
  }

  def getBody(result: play.mvc.Result): Array[Byte] = result.getWrappedResult match {
    case r: AsyncResult => getBody(new ResultWrapper(r.result.await.get))
    case r @ SimpleResult(_, bodyEnumerator) => {
      var readAsBytes = Enumeratee.map[r.BODY_CONTENT](r.writeable.transform(_)).transform(Iteratee.consume[Array[Byte]]())
      bodyEnumerator(readAsBytes).flatMap(_.run)(play.core.Execution.internalContext).value1.get
    }
  }

  class ResultWrapper(r: play.api.mvc.Result) extends play.mvc.Result {
    def getWrappedResult = r
  }

}
