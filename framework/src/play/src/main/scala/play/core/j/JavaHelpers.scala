package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }

import scala.collection.JavaConverters._

/**
 *
 * provides helper methods that manage java to scala Result and scala to java Context
 * creation
 */
trait JavaHelpers {
  import collection.JavaConverters._
  import play.api.mvc._
  import play.mvc.Http.RequestBody

  /**
   * creates a scala result from java context and result objects
   * @param javaContext
   * @param javaResult
   */
  def createResult(javaContext: JContext, javaResult: play.mvc.Result): Result = javaResult.getWrappedResult match {
    case result: PlainResult => {
      val wResult = result.withHeaders(javaContext.response.getHeaders.asScala.toSeq: _*)
        .withCookies((javaContext.response.cookies.asScala.toSeq map { c => Cookie(c.name, c.value, c.maxAge, c.path, Option(c.domain), c.secure, c.httpOnly) }): _*)
        .discardingCookies(javaContext.response.discardedCookies.asScala.toSeq: _*)

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
    case other => other
  }

  /**
   * creates a java context from a scala RequestHeader
   * @param request
   */
  def createJavaContext(req: RequestHeader): JContext = {
    new JContext(new JRequest {

      def uri = req.uri

      def method = req.method

      def host = req.host

      def path = req.path

      def body = null

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def cookies = new JCookies {
        def get(name: String) = (for (cookie <- req.cookies.get(name))
          yield new JCookie(cookie.name, cookie.value, cookie.maxAge, cookie.path, cookie.domain.getOrElse(null), cookie.secure, cookie.httpOnly)).getOrElse(null)
      }

      override def toString = req.toString

    },
      req.session.data.asJava,
      req.flash.data.asJava)
  }

  /**
   * creates a java context from a scala Request[RequestBody]
   * @param request
   */
  def createJavaContext(req: Request[RequestBody]): JContext = {
    new JContext(new JRequest {

      def uri = req.uri

      def method = req.method

      def host = req.host

      def path = req.path

      def body = req.body

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def cookies = new JCookies {
        def get(name: String) = (for (cookie <- req.cookies.get(name))
          yield new JCookie(cookie.name, cookie.value, cookie.maxAge, cookie.path, cookie.domain.getOrElse(null), cookie.secure, cookie.httpOnly)).getOrElse(null)
      }

      override def toString = req.toString

    },
      req.session.data.asJava,
      req.flash.data.asJava)
  }

}
object JavaHelpers extends JavaHelpers
