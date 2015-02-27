/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import play.libs.F
import play.api.libs.iteratee.Execution.trampoline
import play.api.mvc._
import play.mvc.{ Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestImpl => JRequestImpl, RequestHeader => JRequestHeader, Cookies => JCookies, Cookie => JCookie }
import play.mvc.Http.RequestBody

import scala.concurrent.Future
import collection.JavaConverters._

/**
 * Provides helper methods that manage Java to Scala Result and Scala to Java Context
 * creation
 */
trait JavaHelpers {

  def cookiesToScalaCookies(cookies: java.lang.Iterable[play.mvc.Http.Cookie]): Seq[Cookie] = {
    cookies.asScala.toSeq map { c =>
      Cookie(c.name, c.value,
        if (c.maxAge == null) None else Some(c.maxAge), c.path, Option(c.domain), c.secure, c.httpOnly)
    }
  }

  def cookiesToJavaCookies(cookies: Cookies) = {
    new JCookies {
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
  }

  /**
   * Creates a scala result from java context and result objects
   * @param javaContext
   * @param javaResult
   */
  def createResult(javaContext: JContext, javaResult: JResult): Result = {
    val wResult = javaResult.toScala.withHeaders(javaContext.response.getHeaders.asScala.toSeq: _*)
      .withCookies(cookiesToScalaCookies(javaContext.response.cookies): _*)

    if (javaContext.session.isDirty && javaContext.flash.isDirty) {
      wResult.withSession(Session(javaContext.session.asScala.toMap)).flashing(Flash(javaContext.flash.asScala.toMap))
    } else {
      if (javaContext.session.isDirty) {
        wResult.withSession(Session(javaContext.session.asScala.toMap))
      } else {
        if (javaContext.flash.isDirty) {
          wResult.flashing(Flash(javaContext.flash.asScala.toMap))
        } else {
          wResult
        }
      }
    }
  }

  /**
   * Creates a java context from a scala RequestHeader
   * @param req
   */
  def createJavaContext(req: RequestHeader): JContext = {
    new JContext(
      req.id,
      req,
      new JRequestImpl(req),
      req.session.data.asJava,
      req.flash.data.asJava,
      req.tags.mapValues(_.asInstanceOf[AnyRef]).asJava
    )
  }

  /**
   * Creates a java context from a scala Request[RequestBody]
   * @param req
   */
  def createJavaContext(req: Request[RequestBody]): JContext = {
    new JContext(
      req.id,
      req,
      new JRequestImpl(req),
      req.session.data.asJava,
      req.flash.data.asJava,
      req.tags.mapValues(_.asInstanceOf[AnyRef]).asJava)
  }

  /**
   * Invoke the given function with the right context set, converting the scala request to a
   * Java request, and converting the resulting Java result to a Scala result, before returning
   * it.
   *
   * This is intended for use by methods in the JavaGlobalSettingsAdapter, which need to be handled
   * like Java actions, but are not Java actions.
   *
   * @param request The request
   * @param f The function to invoke
   * @return The result
   */
  def invokeWithContextOpt(request: RequestHeader, f: JRequest => F.Promise[JResult]): Option[Future[Result]] = {
    Option(invokeWithContext(request, f))
  }

  /**
   * Invoke the given function with the right context set, converting the scala request to a
   * Java request, and converting the resulting Java result to a Scala result, before returning
   * it.
   *
   * This is intended for use by callback methods in Java adapters.
   *
   * @param request The request
   * @param f The function to invoke
   * @return The result
   */
  def invokeWithContext(request: RequestHeader, f: JRequest => F.Promise[JResult]): Future[Result] = {
    withContext(request) { javaContext =>
      f(javaContext.request()).wrapped.map(createResult(javaContext, _))(trampoline)
    }
  }

  /**
   * Invoke the given block with Java context created from the request header
   */
  def withContext[A](request: RequestHeader)(block: JContext => A) = {
    val javaContext = createJavaContext(request)
    try {
      JContext.current.set(javaContext)
      block(javaContext)
    } finally {
      JContext.current.remove()
    }

  }

}

object JavaHelpers extends JavaHelpers

class RequestHeaderImpl(header: RequestHeader) extends JRequestHeader {

  def _underlyingHeader = header

  def uri = header.uri

  def method = header.method

  def version = header.version

  def remoteAddress = header.remoteAddress

  def secure = header.secure

  def host = header.host

  def path = header.path

  def headers = createHeaderMap(header.headers)

  def acceptLanguages = header.acceptLanguages.map(new play.i18n.Lang(_)).asJava

  def queryString = {
    header.queryString.mapValues(_.toArray).asJava
  }

  def acceptedTypes = header.acceptedTypes.asJava

  def accepts(mediaType: String) = header.accepts(mediaType)

  def cookies = JavaHelpers.cookiesToJavaCookies(header.cookies)

  def getQueryString(key: String): String = {
    if (queryString().containsKey(key) && queryString().get(key).length > 0) queryString().get(key)(0) else null
  }

  def cookie(name: String): JCookie = {
    cookies().get(name)
  }

  def getHeader(headerName: String): String = {
    val header: Array[String] = headers.get(headerName)
    if (header == null) null else header(0)
  }

  def hasHeader(headerName: String): Boolean = {
    getHeader(headerName) != null
  }

  private def createHeaderMap(headers: Headers): java.util.Map[String, Array[String]] = {
    val map = new java.util.TreeMap[String, Array[String]](play.core.utils.CaseInsensitiveOrdered)
    map.putAll(headers.toMap.mapValues(_.toArray).asJava)
    map
  }

  override def toString = header.toString

}
