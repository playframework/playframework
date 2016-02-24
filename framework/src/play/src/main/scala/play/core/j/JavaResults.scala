/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.streams.Streams

import scala.annotation.varargs
import scala.compat.java8.FutureConverters
import scala.language.reflectiveCalls
import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent._
import play.core.Execution.internalContext
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie, Session => JSession, Flash => JFlash }
import play.mvc.{ Result => JResult }
import play.twirl.api.Content
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.function.Consumer

/**
 * Java compatible Results
 */
object JavaResults extends Results with DefaultWriteables with DefaultContentTypeOfs {
  def writeContent(mimeType: String)(implicit codec: Codec): Writeable[Content] =
    Writeable((content: Content) => codec.encode(contentBody(content)), Some(ContentTypes.withCharset(mimeType)))
  def contentBody(content: Content): String = content match { case xml: play.twirl.api.Xml => xml.body.trim; case c => c.body }
  def writeString(mimeType: String)(implicit codec: Codec): Writeable[String] = Writeable((s: String) => codec.encode(s), Some(ContentTypes.withCharset(mimeType)))
  def writeString(implicit codec: Codec): Writeable[String] = writeString(MimeTypes.TEXT)
  def writeBytes: Writeable[Array[Byte]] = Writeable.wByteArray
  def writeBytes(contentType: String): Writeable[ByteString] = Writeable((bs: ByteString) => bs)(contentTypeOfBytes(contentType))
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfBytes(mimeType: String): ContentTypeOf[ByteString] = ContentTypeOf(Option(mimeType).orElse(Some("application/octet-stream")))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def chunked[A](onConnected: Consumer[Channel[A]], onDisconnected: Runnable): Enumerator[A] = {
    Concurrent.unicast[A](
      onStart = (channel: Channel[A]) => onConnected.accept(channel),
      onComplete = onDisconnected.run(),
      onError = (_: String, _: Input[A]) => onDisconnected.run()
    )(internalContext)
  }
  //play.api.libs.iteratee.Enumerator.imperative[A](onComplete = onDisconnected)
  def chunked(stream: java.io.InputStream, chunkSize: Int): Source[ByteString, _] = enumToSource(Enumerator.fromStream(stream, chunkSize)(internalContext))
  def chunked(file: java.io.File, chunkSize: Int) = enumToSource(Enumerator.fromFile(file, chunkSize)(internalContext))
  def chunked(file: java.nio.file.Path, chunkSize: Int) = enumToSource(Enumerator.fromPath(file, chunkSize)(internalContext))
  def sendFile(status: play.api.mvc.Results.Status, file: java.io.File, inline: Boolean, filename: String) = status.sendFile(file, inline, _ => filename)
  def sendPath(status: play.api.mvc.Results.Status, path: java.nio.file.Path, inline: Boolean, filename: String) = status.sendPath(path, inline, _ => filename)
  private def enumToSource(enumerator: Enumerator[Array[Byte]]): Source[ByteString, _] = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
}

object JavaResultExtractor {

  def getCookies(responseHeader: ResponseHeader): JCookies =
    new JCookies {
      private val cookies = Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE))

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

  def getSession(responseHeader: ResponseHeader): JSession =
    new JSession(Session.decodeFromCookie(
      Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE)).get(Session.COOKIE_NAME)
    ).data.asJava)

  def getFlash(responseHeader: ResponseHeader): JFlash = new JFlash(Flash.decodeFromCookie(
    Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE)).get(Flash.COOKIE_NAME)
  ).data.asJava)

  def withHeader(responseHeader: ResponseHeader, name: String, value: String): ResponseHeader =
    responseHeader.copy(headers = responseHeader.headers + (name -> value))

  @varargs
  def withHeader(responseHeader: ResponseHeader, nameValues: String*): ResponseHeader = {
    if (nameValues.length % 2 != 0) {
      throw new IllegalArgumentException("Unmatched name - withHeaders must be invoked with an even number of string arguments")
    }
    val toAdd = nameValues.grouped(2).map(pair => pair(0) -> pair(1))
    responseHeader.copy(headers = responseHeader.headers ++ toAdd)
  }

  def getBody(result: JResult, timeout: Long, materializer: Materializer): ByteString =
    Await.result(FutureConverters.toScala(result.body.consumeData(materializer)), timeout.millis)

}
