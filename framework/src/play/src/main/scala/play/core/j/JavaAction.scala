package play.core.j

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }

/*
 * An action that's handling Java requests
 */
trait JavaAction extends Action[play.mvc.Http.RequestBody] with JavaHelpers {

  def parser = {
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map { bodyParserOf =>
        bodyParserOf.value.newInstance.parser(bodyParserOf.maxLength)
      }.getOrElse(JavaParsers.anyContent(java.lang.Integer.MAX_VALUE))
  }

  def invocation: JResult
  def controller: Class[_]
  def method: java.lang.reflect.Method

  def apply(req: Request[play.mvc.Http.RequestBody]): Result = {

    val javaContext = createJavaContext(req)

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

    createResult(javaContext, finalAction.call(javaContext))
  }

}