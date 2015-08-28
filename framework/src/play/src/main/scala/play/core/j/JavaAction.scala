/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.{ HttpConfiguration, HttpRequestHandler }
import play.api.inject.{ Injector, NewInstanceInjector }

import scala.language.existentials

import play.api.libs.iteratee.Execution.trampoline
import play.api.mvc._
import play.mvc.{ Action => JAction, Result => JResult, BodyParser => JBodyParser }
import play.mvc.Http.{ Context => JContext }
import play.libs.F.{ Promise => JPromise }
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Retains and evaluates what is otherwise expensive reflection work on call by call basis.
 * @param controller The controller to be evaluated
 * @param method     The method to be evaluated
 */
class JavaActionAnnotations(val controller: Class[_], val method: java.lang.reflect.Method) {

  val parser: Class[_ <: JBodyParser[_]] =
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map(_.value).getOrElse(classOf[JBodyParser.Default])

  val controllerAnnotations = play.api.libs.Collections.unfoldLeft[Seq[java.lang.annotation.Annotation], Option[Class[_]]](Option(controller)) { clazz =>
    clazz.map(c => (Option(c.getSuperclass), c.getDeclaredAnnotations.toSeq))
  }.flatten

  val actionMixins = {
    val allDeclaredAnnotations: Seq[java.lang.annotation.Annotation] = if (HttpConfiguration.current.actionComposition.controllerAnnotationsFirst) {
      (controllerAnnotations ++ method.getDeclaredAnnotations)
    } else {
      (method.getDeclaredAnnotations ++ controllerAnnotations)
    }
    allDeclaredAnnotations.collect {
      case a: play.mvc.With => a.value.map(c => (a, c)).toSeq
      case a if a.annotationType.isAnnotationPresent(classOf[play.mvc.With]) =>
        a.annotationType.getAnnotation(classOf[play.mvc.With]).value.map(c => (a, c)).toSeq
    }.flatten.reverse
  }
}

/*
 * An action that's handling Java requests
 */
abstract class JavaAction(components: JavaHandlerComponents) extends Action[play.mvc.Http.RequestBody] with JavaHelpers {

  def invocation: JPromise[JResult]
  val annotations: JavaActionAnnotations

  def apply(req: Request[play.mvc.Http.RequestBody]): Future[Result] = {

    val javaContext: JContext = createJavaContext(req)

    val rootAction = new JAction[Any] {
      def call(ctx: JContext): JPromise[JResult] = {
        // The context may have changed, set it again
        val oldContext = JContext.current.get()
        try {
          JContext.current.set(ctx)
          invocation
        } finally {
          JContext.current.set(oldContext)
        }
      }
    }

    val baseAction = components.requestHandler.createAction(javaContext.request, annotations.method)
    baseAction.delegate = rootAction

    val finalAction = components.requestHandler.wrapAction(
      annotations.actionMixins.foldLeft[JAction[_ <: Any]](baseAction) {
        case (delegate, (annotation, actionClass)) =>
          val action = components.injector.instanceOf(actionClass).asInstanceOf[play.mvc.Action[Object]]
          action.configuration = annotation
          action.delegate = delegate
          action
      }
    )

    val trampolineWithContext: ExecutionContext = {
      val javaClassLoader = Thread.currentThread.getContextClassLoader
      new HttpExecutionContext(javaClassLoader, javaContext, trampoline)
    }
    val actionFuture: Future[Future[JResult]] = Future { finalAction.call(javaContext).wrapped }(trampolineWithContext)
    val flattenedActionFuture: Future[JResult] = actionFuture.flatMap(identity)(trampoline)
    val resultFuture: Future[Result] = flattenedActionFuture.map(createResult(javaContext, _))(trampoline)
    resultFuture
  }

}

/**
 * A Java handler.
 *
 * Java handlers, given that they have to load actions and perform Java specific interception, need extra components
 * that can't be supplied by the controller itself to do so.  So this handler is a factory for handlers that, given
 * the JavaComponents, will return a handler that can be invoked by a Play server.
 */
trait JavaHandler extends Handler {

  /**
   * Return a Handler that has the necessary components supplied to execute it.
   */
  def withComponents(components: JavaHandlerComponents): Handler
}

/**
 * The components necessary to handle a Java handler.
 */
class JavaHandlerComponents @Inject() (val injector: Injector,
  val requestHandler: play.http.HttpRequestHandler)

