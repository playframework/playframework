/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import akka.stream.Materializer
import akka.util.ByteString

import scala.annotation.varargs
import scala.compat.java8.FutureConverters
import scala.language.reflectiveCalls
import play.api.mvc._
import play.api.http._
import play.mvc.Http.{ Cookies => JCookies, Cookie => JCookie, Session => JSession, Flash => JFlash }
import play.mvc.{ Result => JResult }
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

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
