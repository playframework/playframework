/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.util.concurrent.CompletionStage
import javax.inject.Inject

import play.api.http.{ ActionCompositionConfiguration, HttpConfiguration }
import play.api.inject.Injector

import scala.compat.java8.FutureConverters
import scala.language.existentials
import play.core.Execution.Implicits.trampoline
import play.api.mvc._
import play.mvc.{ FileMimeTypes, Action => JAction, BodyParser => JBodyParser, Result => JResult }
import play.i18n.{ Langs => JLangs, MessagesApi => JMessagesApi }
import play.mvc.Http.{ Context => JContext }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Retains and evaluates what is otherwise expensive reflection work on call by call basis.
 *
 * @param controller The controller to be evaluated
 * @param method     The method to be evaluated
 */
class JavaActionAnnotations(val controller: Class[_], val method: java.lang.reflect.Method, config: ActionCompositionConfiguration) {
  val parser: Class[_ <: JBodyParser[_]] =
    Seq(method.getAnnotation(classOf[play.mvc.BodyParser.Of]), controller.getAnnotation(classOf[play.mvc.BodyParser.Of]))
      .filterNot(_ == null)
      .headOption.map(_.value).getOrElse(classOf[JBodyParser.Default])

  val controllerAnnotations = play.api.libs.Collections.unfoldLeft[Seq[java.lang.annotation.Annotation], Option[Class[_]]](Option(controller)) { clazz =>
    clazz.map(c => (Option(c.getSuperclass), c.getDeclaredAnnotations.toSeq))
  }.flatten

  val actionMixins = {
    val allDeclaredAnnotations: Seq[java.lang.annotation.Annotation] = if (config.controllerAnnotationsFirst) {
      controllerAnnotations ++ method.getDeclaredAnnotations
    } else {
      method.getDeclaredAnnotations ++ controllerAnnotations
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
abstract class JavaAction(val handlerComponents: JavaHandlerComponents)
    extends Action[play.mvc.Http.RequestBody] with JavaHelpers {
  private def config: ActionCompositionConfiguration = handlerComponents.httpConfiguration.actionComposition

  def invocation: CompletionStage[JResult]
  val annotations: JavaActionAnnotations

  val executionContext: ExecutionContext = handlerComponents.executionContext

  def apply(req: Request[play.mvc.Http.RequestBody]): Future[Result] = {
    val contextComponents = handlerComponents.contextComponents
    val javaContext: JContext = createJavaContext(req, contextComponents)

    val rootAction = new JAction[Any] {
      def call(ctx: JContext): CompletionStage[JResult] = {
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

    val baseAction = handlerComponents.actionCreator.createAction(javaContext.request, annotations.method)

    val endOfChainAction = if (config.executeActionCreatorActionFirst) {
      rootAction
    } else {
      baseAction.delegate = rootAction
      baseAction
    }

    val finalUserDeclaredAction = annotations.actionMixins.foldLeft[JAction[_ <: Any]](endOfChainAction) {
      case (delegate, (annotation, actionClass)) =>
        val action = handlerComponents.getAction(actionClass).asInstanceOf[play.mvc.Action[Object]]
        action.configuration = annotation
        action.delegate = delegate
        action
    }

    val finalAction = handlerComponents.actionCreator.wrapAction(if (config.executeActionCreatorActionFirst) {
      baseAction.delegate = finalUserDeclaredAction
      baseAction
    } else {
      finalUserDeclaredAction
    })

    val trampolineWithContext: ExecutionContext = {
      val javaClassLoader = Thread.currentThread.getContextClassLoader
      new HttpExecutionContext(javaClassLoader, javaContext, trampoline)
    }
    val actionFuture: Future[Future[JResult]] = Future { FutureConverters.toScala(finalAction.call(javaContext)) }(trampolineWithContext)
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
  def withComponents(handlerComponents: JavaHandlerComponents): Handler
}

trait JavaContextComponents {
  def messagesApi: JMessagesApi
  def langs: JLangs
  def fileMimeTypes: FileMimeTypes
  def httpConfiguration: HttpConfiguration
}

/**
 * The components necessary to handle a play.mvc.Http.Context object.
 */
class DefaultJavaContextComponents @Inject() (
  val messagesApi: JMessagesApi,
  val langs: JLangs,
  val fileMimeTypes: FileMimeTypes,
  val httpConfiguration: HttpConfiguration
) extends JavaContextComponents

trait JavaHandlerComponents {
  def getBodyParser[A <: JBodyParser[_]](parserClass: Class[A]): A
  def getAction[A <: JAction[_]](actionClass: Class[A]): A
  def actionCreator: play.http.ActionCreator
  def httpConfiguration: HttpConfiguration
  def executionContext: ExecutionContext
  def contextComponents: JavaContextComponents
}

/**
 * The components necessary to handle a Java handler.
 */
class DefaultJavaHandlerComponents @Inject() (
    injector: Injector,
    val actionCreator: play.http.ActionCreator,
    val httpConfiguration: HttpConfiguration,
    val executionContext: ExecutionContext,
    val contextComponents: JavaContextComponents
) extends JavaHandlerComponents {
  def getBodyParser[A <: JBodyParser[_]](parserClass: Class[A]): A = injector.instanceOf(parserClass)
  def getAction[A <: JAction[_]](actionClass: Class[A]): A = injector.instanceOf(actionClass)
}

