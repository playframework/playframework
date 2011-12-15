package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody }

import scala.collection.JavaConverters._

trait JavaAction extends Action[play.mvc.Http.RequestBody] {

  def parser = {
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map { bodyParserOf =>
        bodyParserOf.value.newInstance.parser(bodyParserOf.maxLength)
      }.getOrElse(JParsers.anyContent(-1))
  }

  def invocation: JResult
  def controller: Class[_]
  def method: java.lang.reflect.Method

  def apply(req: Request[play.mvc.Http.RequestBody]) = {

    val javaContext = new JContext(

      new JRequest {

        def uri = req.uri
        def method = req.method
        def path = req.method

        lazy val queryString = {
          req.queryString.mapValues(_.toArray).asJava
        }

        def body = req.body

        override def toString = req.toString

      },

      req.session.data.asJava,
      req.flash.data.asJava)

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
      case result @ SimpleResult(_, _) => {
        val wResult = result.withHeaders(javaContext.response.getHeaders.asScala.toSeq: _*)

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

  /*
   * converts a Java action into a scala one
   * @param java result
   */
  def toAction(r: play.mvc.Result) = Action { r.getWrappedResult }

  /**
   * provides user defined request
   * @param uri
   * @param method
   * @param queryString
   * @param body
   * @param username
   * @param path
   * @param headers
   * @param cookies
   */

  def toRequest(_uri: String, _method: String, _queryString: java.util.Map[String, Seq[String]],
    _body: java.util.Map[String, Seq[String]], _username: String, _path: String, _headers: java.util.Map[String, Array[String]], _cookies: java.util.Map[String, String]) = new play.api.mvc.Request[AnyContent] {
    def uri = _uri
    def method = _method
    def queryString = _queryString.asScala.toMap
    def body: AnyContent = AnyContentAsUrlFormEncoded(_body.asScala.toMap).asInstanceOf[AnyContent]

    def username = if (_username == null) None else Some(_username)
    def path = _path

    def headers = new Headers {
      def getAll(key: String) = _headers.asScala.toMap.get(key).getOrElse(null)
      def keys = _headers.asScala.toMap.keySet
    }
    def cookies = new Cookies {
      def get(name: String) = {
        val n = name
        _cookies.asScala.toMap.get(n).map(c => Cookie(name = n, value = c))
      }
    }
  }
}
