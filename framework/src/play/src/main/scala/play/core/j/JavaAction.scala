package play.core.j

import scala.language.existentials

import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult }
import play.mvc.Http.{ Context => JContext, Request => JRequest, RequestBody => JBody, Cookies => JCookies, Cookie => JCookie }

/**
 * Retains and evaluates what is otherwise expensive reflection work on call by call basis.
 * @param controller The controller to be evaluated
 * @param method     The method to be evaluated
 */
class JavaActionAnnotations(val controller: Class[_], val method: java.lang.reflect.Method) {

  val parser: BodyParser[play.mvc.Http.RequestBody] =
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map { bodyParserOf =>
        bodyParserOf.value.newInstance.parser(bodyParserOf.maxLength)
      }.getOrElse(JavaParsers.anyContent(java.lang.Integer.MAX_VALUE))

  val controllerAnnotations = play.api.libs.Collections.unfoldLeft[Seq[java.lang.annotation.Annotation], Option[Class[_]]](Option(controller)) { clazz =>
    clazz.map(c => (Option(c.getSuperclass), c.getDeclaredAnnotations.toSeq))
  }.flatten

  val actionMixins = {
    (method.getDeclaredAnnotations ++ controllerAnnotations).collect {
      case a: play.mvc.With => a.value.map(c => (a, c)).toSeq
      case a if a.annotationType.isAnnotationPresent(classOf[play.mvc.With]) => {
        a.annotationType.getAnnotation(classOf[play.mvc.With]).value.map(c => (a, c)).toSeq
      }
    }.flatten.reverse
  }
}

/*
 * An action that's handling Java requests
 */
trait JavaAction extends Action[play.mvc.Http.RequestBody] with JavaHelpers {

  def invocation: JResult
  val annotations: JavaActionAnnotations

  def apply(req: Request[play.mvc.Http.RequestBody]): Result = {

    val javaContext = createJavaContext(req)

    val rootAction = new JAction[Any] {
      def call(ctx: JContext): JResult = invocation
    }

    // Wrap into user defined Global action
    val baseAction = play.api.Play.maybeApplication.map { app =>
      app.global match {
        case global: JavaGlobalSettingsAdapter => {
          val action = global.underlying.onRequest(javaContext.request, annotations.method)
          action.delegate = rootAction
          action
        }
        case _ => rootAction
      }
    }.getOrElse(rootAction)

    val finalAction = annotations.actionMixins.foldLeft[JAction[_ <: Any]](baseAction) {
      case (delegate, (annotation, actionClass)) => {
        val global = play.api.Play.maybeApplication.map(_.global).getOrElse(play.api.DefaultGlobal)
        val action = global.getControllerInstance(actionClass).asInstanceOf[play.mvc.Action[Object]]
        action.configuration = annotation
        action.delegate = delegate
        action
      }
    }

    createResult(javaContext, try {
      JContext.current.set(javaContext)
      play.mvc.Results.async {
        play.libs.F.Promise.pure("").map(
          new play.libs.F.Function[String,JResult] {
            def apply(nothing: String) = finalAction.call(javaContext)
          }
        )
      }
    } finally {
      JContext.current.remove()
    })

  }

}
