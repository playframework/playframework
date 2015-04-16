/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import scala.language.reflectiveCalls

import com.fasterxml.jackson.databind.JsonNode
import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent._
import play.api.libs.iteratee.Execution.trampoline
import play.core.Execution.internalContext
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie, Session => JSession, Flash => JFlash }
import play.mvc.{ Result => JResult }
import play.twirl.api.Content
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Java compatible Results
 */
object JavaResults extends Results with DefaultWriteables with DefaultContentTypeOfs {
  def writeContent(mimeType: String)(implicit codec: Codec): Writeable[Content] =
    Writeable((content: Content) => codec.encode(contentBody(content)), Some(ContentTypes.withCharset(mimeType)))(trampoline)
  def contentBody(content: Content): String = content match { case xml: play.twirl.api.Xml => xml.body.trim; case c => c.body }
  def writeString(mimeType: String)(implicit codec: Codec): Writeable[String] = Writeable((s: String) => codec.encode(s), Some(ContentTypes.withCharset(mimeType)))(trampoline)
  def writeString(implicit codec: Codec): Writeable[String] = writeString(MimeTypes.TEXT)
  def writeJson(implicit codec: Codec): Writeable[JsonNode] = Writeable((json: JsonNode) => codec.encode(json.toString), Some(ContentTypes.JSON))(trampoline)
  def writeBytes: Writeable[Array[Byte]] = Writeable.wBytes
  def writeBytes(contentType: String): Writeable[Array[Byte]] = Writeable((bs: Array[Byte]) => bs)(contentTypeOfBytes(contentType), trampoline)
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfBytes(mimeType: String): ContentTypeOf[Array[Byte]] = ContentTypeOf(Option(mimeType).orElse(Some("application/octet-stream")))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def chunked[A](onConnected: play.libs.F.Callback[Channel[A]], onDisconnected: play.libs.F.Callback0): Enumerator[A] = {
    Concurrent.unicast[A](
      onStart = (channel: Channel[A]) => onConnected.invoke(channel),
      onComplete = onDisconnected.invoke(),
      onError = (_: String, _: Input[A]) => onDisconnected.invoke()
    )(internalContext)
  }
  //play.api.libs.iteratee.Enumerator.imperative[A](onComplete = onDisconnected)
  def chunked(stream: java.io.InputStream, chunkSize: Int): Enumerator[Array[Byte]] = Enumerator.fromStream(stream, chunkSize)(internalContext)
  def chunked(file: java.io.File, chunkSize: Int) = Enumerator.fromFile(file, chunkSize)(internalContext)
  def sendFile(status: play.api.mvc.Results.Status, file: java.io.File, inline: Boolean, filename: String) = status.sendFile(file, inline, _ => filename)
}

object JavaResultExtractor {

  def getCookies(result: JResult): JCookies =
    new JCookies {
      private val cookies = Cookies(headers(result).get(HeaderNames.SET_COOKIE))

      def get(name: String): JCookie = {
        cookies.get(name).map(makeJavaCookie).orNull
      }

      private def makeJavaCookie(cookie: Cookie): JCookie = {
        new JCookie(cookie.name,
          cookie.value,
          cookie.maxAge.map(i => new Integer(i)).orNull,
          cookie.path,
          cookie.domain.orNull,
          cookie.secure,
          cookie.httpOnly)
      }

      def iterator: java.util.Iterator[JCookie] = {
        cookies.toIterator.map(makeJavaCookie).asJava
      }
    }

  def getSession(result: JResult): JSession =
    new JSession(Session.decodeFromCookie(
      Cookies(headers(result).get(HeaderNames.SET_COOKIE)).get(Session.COOKIE_NAME)
    ).data.asJava)

  def getFlash(result: JResult): JFlash = new JFlash(Flash.decodeFromCookie(
    Cookies(headers(result).get(HeaderNames.SET_COOKIE)).get(Flash.COOKIE_NAME)
  ).data.asJava)

  def getHeaders(result: JResult): java.util.Map[String, String] = headers(result).asJava

  def getBody(result: JResult, timeout: Long): Array[Byte] =
    Await.result(result.toScala.body |>>> Iteratee.consume[Array[Byte]](), timeout.millis)

  private def headers(result: JResult) = result.toScala.header.headers

}
