/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import akka.stream.Materializer
import akka.util.ByteString
import play.api.http._
import play.api.mvc._
import play.mvc.Http.{ Cookie => JCookie, Cookies => JCookies, Flash => JFlash, Session => JSession }
import play.mvc.{ Result => JResult }
import play.mvc.{ ResponseHeader => JResponseHeader }

import scala.annotation.varargs
import scala.collection.JavaConverters
import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.reflectiveCalls

object JavaResultExtractor {

  def getCookies(responseHeader: ResponseHeader): JCookies =
    new JCookies {
      private val cookies = Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE))

      def get(name: String): JCookie = {
        cookies.get(name).map(makeJavaCookie).orNull
      }

      private def makeJavaCookie(cookie: Cookie): JCookie = {
        new JCookie(
          cookie.name,
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

  def withCookies(header: ResponseHeader, cookies: Array[JCookie]): ResponseHeader = {
    if (cookies.isEmpty) {
      header
    } else {
      val cookieHeader = Cookies.mergeSetCookieHeader(
        header.headers.getOrElse(HeaderNames.SET_COOKIE, ""),
        JavaHelpers.cookiesToScalaCookies(java.util.Arrays.asList(cookies: _*))
      )
      header.copy(headers = header.headers + (HeaderNames.SET_COOKIE -> cookieHeader))
    }
  }

  def getSession(responseHeader: ResponseHeader): JSession =
    new JSession(Session.decodeFromCookie(
      Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE)).get(Session.COOKIE_NAME)
    ).data.asJava)

  def getFlash(responseHeader: ResponseHeader): JFlash = new JFlash(Flash.decodeFromCookie(
    Cookies.fromSetCookieHeader(responseHeader.headers.get(HeaderNames.SET_COOKIE)).get(Flash.COOKIE_NAME)
  ).data.asJava)

  @varargs
  def withHeader(responseHeader: JResponseHeader, nameValues: String*): JResponseHeader = {
    import JavaConverters._
    if (nameValues.length % 2 != 0) {
      throw new IllegalArgumentException("Unmatched name - withHeaders must be invoked with an even number of string arguments")
    }
    val toAdd = nameValues.grouped(2).map(pair => pair(0) -> pair(1))
    responseHeader.withHeaders(toAdd.toMap.asJava)
  }

  def getBody(result: JResult, timeout: Long, materializer: Materializer): ByteString =
    Await.result(FutureConverters.toScala(result.body.consumeData(materializer)), timeout.millis)

}
