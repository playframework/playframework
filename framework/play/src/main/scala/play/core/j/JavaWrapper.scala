package play.core.j

import scala.collection.JavaConverters._

import play.api.mvc._

import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest }

trait JavaAction extends Action[AnyContent] {

  def parser = BodyParsers.parse.anyContent

  def invocation: JResult
  def controller: Class[_]
  def method: java.lang.reflect.Method

  def apply(req: Request[AnyContent]) = {

    val javaContext = new JContext(

      new JRequest {

        def uri = req.uri
        def method = req.method
        def path = req.method

        def queryString = {
          req.queryString.mapValues(_.toArray).asJava
        }

        def urlFormEncoded = {
          req.body.asUrlFormEncoded.getOrElse(Map.empty).mapValues(_.toArray).asJava
        }

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
  def toAction(r: play.mvc.Result) = Action { r.getWrappedResult }
}
