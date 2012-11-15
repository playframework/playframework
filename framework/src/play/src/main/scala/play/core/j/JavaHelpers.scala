package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }

import scala.collection.JavaConverters._


class EitherToFEither[A,B]() extends play.libs.F.Function[Either[A,B],play.libs.F.Either[A,B]] {

  def apply(e:Either[A,B]): play.libs.F.Either[A,B] = e.fold(play.libs.F.Either.Left(_), play.libs.F.Either.Right(_))

}

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
        .withCookies((javaContext.response.cookies.asScala.toSeq map { c => Cookie(c.name, c.value,
          if (c.maxAge == null) None else Some(c.maxAge), c.path, Option(c.domain), c.secure, c.httpOnly) }): _*)

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
   * creates a java request (with an empty body) from a scala RequestHeader
   * @param request incoming requestHeader
   */
  def createJavaRequest(req: RequestHeader): JRequest = {
    new JRequest {

      def uri = req.uri

      def method = req.method

      def version = req.version

      def remoteAddress = req.remoteAddress

      def host = req.host

      def path = req.path

      def body = null

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def accept = req.accept.asJava

      def accepts(mediaType: String) = req.accepts(mediaType)

      def cookies = new JCookies {
        def get(name: String) = (for (cookie <- req.cookies.get(name))
          yield new JCookie(cookie.name, cookie.value, cookie.maxAge.map(i => new Integer(i)).orNull, cookie.path, cookie.domain.orNull, cookie.secure, cookie.httpOnly)).getOrElse(null)
      }

      override def toString = req.toString

    }
  }

  /**
   * creates a java context from a scala RequestHeader
   * @param request
   */
  def createJavaContext(req: RequestHeader): JContext = {
    new JContext(
      req.id,
      req,
      createJavaRequest(req),
      req.session.data.asJava,
      req.flash.data.asJava,
      req.tags.mapValues(_.asInstanceOf[AnyRef]).asJava
    )
  }

  /**
   * creates a java context from a scala Request[RequestBody]
   * @param request
   */
  def createJavaContext(req: Request[RequestBody]): JContext = {
    new JContext(req.id, req, new JRequest {

      def uri = req.uri

      def method = req.method

      def version = req.version

      def remoteAddress = req.remoteAddress

      def host = req.host

      def path = req.path

      def body = req.body

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

      def acceptLanguages = req.acceptLanguages.map(new play.i18n.Lang(_)).asJava

      def accept = req.accept.asJava

      def accepts(mediaType: String) = req.accepts(mediaType)

      def queryString = {
        req.queryString.mapValues(_.toArray).asJava
      }

      def cookies = new JCookies {
        def get(name: String) = (for (cookie <- req.cookies.get(name))
          yield new JCookie(cookie.name, cookie.value, cookie.maxAge.map(i => new Integer(i)).orNull, cookie.path, cookie.domain.orNull, cookie.secure, cookie.httpOnly)).getOrElse(null)
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
  def invokeWithContext(request: RequestHeader, f: JRequest => Option[JResult]): Option[Result] = {
    val javaContext = createJavaContext(request)
    try {
      JContext.current.set(javaContext)
      f(javaContext.request()).map(result => createResult(javaContext, result))
    } finally {
      JContext.current.remove()
    }
  }

}
object JavaHelpers extends JavaHelpers
