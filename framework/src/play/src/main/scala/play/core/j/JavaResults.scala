package play.core.j

import scala.language.reflectiveCalls

import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent._

import scala.collection.JavaConverters._
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie, Session => JSession, Flash => JFlash }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Java compatible Results
 */
object JavaResults extends Results with DefaultWriteables with DefaultContentTypeOfs {
  def writeContent(mimeType: String)(implicit codec: Codec): Writeable[Content] = Writeable(content => codec.encode(content.body), Some(ContentTypes.withCharset(mimeType)))
  def writeString(mimeType: String)(implicit codec: Codec): Writeable[String] = Writeable(s => codec.encode(s), Some(ContentTypes.withCharset(mimeType)))
  def writeString(implicit codec: Codec): Writeable[String] = writeString(MimeTypes.TEXT)
  def writeJson(implicit codec: Codec): Writeable[com.fasterxml.jackson.databind.JsonNode] = Writeable(json => codec.encode(json.toString), Some(ContentTypes.JSON))
  def writeBytes: Writeable[Array[Byte]] = Writeable.wBytes
  def writeBytes(contentType: String): Writeable[Array[Byte]] = Writeable((bs: Array[Byte]) => bs)(contentTypeOfBytes(contentType))
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfBytes(mimeType: String): ContentTypeOf[Array[Byte]] = ContentTypeOf(Option(mimeType).orElse(Some("application/octet-stream")))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def async(p: scala.concurrent.Future[Result]) = AsyncResult(p)
  def chunked[A](onConnected: play.libs.F.Callback[Channel[A]], onDisconnected: play.libs.F.Callback0): Enumerator[A] = {
    val (enumerator, channel) = Concurrent.broadcast[A]
    new Enumerator[A] {
      def apply[C](i: Iteratee[A, C]) = {
          onConnected.invoke(channel)
          enumerator.onDoneEnumerating(onDisconnected.invoke())(play.core.Execution.internalContext).apply(i)
      }
    }
  }
  //play.api.libs.iteratee.Enumerator.imperative[A](onComplete = onDisconnected)
  def chunked(stream: java.io.InputStream, chunkSize: Int): Enumerator[Array[Byte]] = Enumerator.fromStream(stream, chunkSize)
  def chunked(file: java.io.File, chunkSize: Int) = Enumerator.fromFile(file, chunkSize)
  def chunkedStrategy = StreamingStrategy.Chunked()
  def simpleStrategy = StreamingStrategy.Simple

}

object JavaResultExtractor {

  def getCookies(result: play.mvc.SimpleResult): JCookies =
    new JCookies {
      def get(name: String) = {
        Cookies(headers(result).get(HeaderNames.SET_COOKIE)).get(name).map { cookie =>
          new JCookie(cookie.name, cookie.value, cookie.maxAge.map(i => new Integer(i)).orNull, cookie.path, cookie.domain.orNull, cookie.secure, cookie.httpOnly)
        }.getOrElse(null)
      }
    }

  def getSession(result: play.mvc.SimpleResult): JSession =
    new JSession(Session.decodeFromCookie(
      Cookies(headers(result).get(HeaderNames.SET_COOKIE)).get(Session.COOKIE_NAME)
    ).data.asJava)

  def getFlash(result: play.mvc.SimpleResult): JFlash = new JFlash(Flash.decodeFromCookie(
      Cookies(headers(result).get(HeaderNames.SET_COOKIE)).get(Flash.COOKIE_NAME)
    ).data.asJava)

  def getHeaders(result: play.mvc.SimpleResult): java.util.Map[String, String] = headers(result).asJava

  def getBody(result: play.mvc.SimpleResult): Array[Byte] =
      Await.result(result.getWrappedSimpleResult.body |>>> Iteratee.consume[Array[Byte]](), Duration.Inf)

  private def headers(result: play.mvc.SimpleResult) = result.getWrappedSimpleResult.header.headers

}
