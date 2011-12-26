package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }

import scala.collection.JavaConverters._

object JavaWebSocket {

  def ofString(retrieveWebSocket: => play.mvc.WebSocket[String]) = WebSocket[String] { request =>
    (in, out) =>

      import play.api.libs.iteratee._

      val javaWebSocket = try {
        JContext.current.set(Wrap.createJavaContext(request))
        retrieveWebSocket
      } finally {
        JContext.current.remove()
      }

      val enumerator = new CallbackEnumerator[String]

      val socketOut = new play.mvc.WebSocket.Out[String](enumerator)
      val socketIn = new play.mvc.WebSocket.In[String]

      in |>> Iteratee.mapChunk_((msg: String) => socketIn.callbacks.asScala.foreach(_.invoke(msg)))

      enumerator |>> out

      javaWebSocket.onReady(socketIn, socketOut)

  }

}

trait JavaAction extends Action[play.mvc.Http.RequestBody] {

  def parser = {
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map { bodyParserOf =>
        bodyParserOf.value.newInstance.parser(bodyParserOf.maxLength)
      }.getOrElse(JParsers.anyContent(java.lang.Integer.MAX_VALUE))
  }

  def invocation: JResult
  def controller: Class[_]
  def method: java.lang.reflect.Method

  def apply(req: Request[play.mvc.Http.RequestBody]) = {

    val javaContext = Wrap.createJavaContext(req)

    val rootAction = new JAction[Any] {

      def call(ctx: JContext): JResult = {
        try {
          JContext.current.set(ctx)
          invocation
        } finally {
          JContext.current.remove()
        }
      }
    }

    val actionMixins = {
      (method.getDeclaredAnnotations ++ controller.getDeclaredAnnotations)
        .filter(_.annotationType.isAnnotationPresent(classOf[play.mvc.With]))
        .map(a => a -> a.annotationType.getAnnotation(classOf[play.mvc.With]).value())
        .reverse
    }

    val finalAction = actionMixins.foldLeft(rootAction) {
      case (deleguate, (annotation, actionClass)) => {
        val action = actionClass.newInstance().asInstanceOf[JAction[Any]]
        action.configuration = annotation
        action.deleguate = deleguate
        action
      }
    }

    finalAction.call(javaContext).getWrappedResult match {

      case result: PlainResult => {
        import collection.JavaConverters._
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
  }

}
/**
 * wrap a java result into an Action
 */
object Wrap {
  import collection.JavaConverters._
  import play.api.mvc._
  import play.mvc.Http.RequestBody

  /**
   * creates a context for java apps
   * @param request
   */
  def createJavaContext(req: RequestHeader) = {
    new JContext(new JRequest {

      def uri = req.uri
      def method = req.method
      def path = req.path

      def body = null

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

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
   * creates a context for java apps
   * @param request
   */
  def createJavaContext(req: Request[RequestBody]) = {
    new JContext(new JRequest {

      def uri = req.uri
      def method = req.method
      def path = req.method

      def body = req.body

      def headers = req.headers.toMap.map(e => e._1 -> e._2.toArray).asJava

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
