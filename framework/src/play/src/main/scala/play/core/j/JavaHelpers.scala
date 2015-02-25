/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import play.mvc.{ Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, Cookies => JCookies, Cookie => JCookie }

import play.libs.F
import scala.concurrent.Future
import play.api.libs.iteratee.Execution.trampoline

/**
 * Provides helper methods that manage Java to Scala Result and Scala to Java Context
 * creation
 */
trait JavaHelpers {
  import collection.JavaConverters._
  import play.api.mvc._
  import play.mvc.Http.RequestBody

  /**
   * Creates a scala result from java context and result objects
   * @param javaContext
   * @param javaResult
   */
  def createResult(javaContext: JContext, javaResult: JResult): Result = {
    val wResult = javaResult.toScala.withHeaders(javaContext.response.getHeaders.asScala.toSeq: _*)
      .withCookies(javaContext.response.cookies.asScala.toSeq map { c =>
        Cookie(c.name, c.value,
          if (c.maxAge == null) None else Some(c.maxAge), c.path, Option(c.domain), c.secure, c.httpOnly)
      }: _*)

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
   * Creates a java request (with an empty body) from a scala RequestHeader
   * @param req incoming requestHeader
   */
  def createJavaRequest(req: RequestHeader): JRequest = {
    new JRequest {

      def uri = req.uri

      def method = req.method

      def version = req.version

      def remoteAddress = req.remoteAddress

      def secure = req.secure

      def host = req.host

      def path = req.path

      def body = null

      def headers = createHeaderMap(req.headers)

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def acceptedTypes = req.acceptedTypes.asJava

      def accepts(mediaType: String) = req.accepts(mediaType)

      def cookies = new JCookies {
        def get(name: String): JCookie = {
          req.cookies.get(name).map(makeJavaCookie).orNull
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
          req.cookies.toIterator.map(makeJavaCookie).asJava
        }
      }

      override def toString = req.toString

    }
  }

  /**
   * Creates a java context from a scala RequestHeader
   * @param req
   */
  def createJavaContext(req: RequestHeader): JContext = {
    new JContext(
      req,
      createJavaRequest(req),
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
    new JContext(req, new JRequest {

      def uri = req.uri

      def method = req.method

      def version = req.version

      def remoteAddress = req.remoteAddress

      def secure = req.secure

      def host = req.host

      def path = req.path

      def body = req.body

      def headers = createHeaderMap(req.headers)

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def acceptedTypes = req.acceptedTypes.asJava

      def accepts(mediaType: String) = req.accepts(mediaType)

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def cookies = new JCookies {
        def get(name: String): JCookie = {
          req.cookies.get(name).map(makeJavaCookie).orNull
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
          req.cookies.toIterator.map(makeJavaCookie).asJava
        }
      }

      override def toString = req.toString

    },
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

  private def createHeaderMap(headers: Headers): java.util.Map[String, Array[String]] = {
    val map = new java.util.TreeMap[String, Array[String]](play.core.utils.CaseInsensitiveOrdered)
    map.putAll(headers.toMap.mapValues(_.toArray).asJava)
    map
  }

}
object JavaHelpers extends JavaHelpers
