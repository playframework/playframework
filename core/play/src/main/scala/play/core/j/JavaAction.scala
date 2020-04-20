/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.lang.annotation.Annotation
import java.lang.reflect.AnnotatedElement;
import java.util.concurrent.CompletionStage
import javax.inject.Inject

import play.api.http.ActionCompositionConfiguration
import play.api.http.HttpConfiguration
import play.api.inject.Injector
import play.api.Logger

import scala.compat.java8.FutureConverters
import scala.language.existentials
import play.core.Execution.Implicits.trampoline
import play.api.mvc._
import play.mvc.FileMimeTypes
import play.mvc.{ Action => JAction }
import play.mvc.{ BodyParser => JBodyParser }
import play.mvc.{ Result => JResult }
import play.i18n.{ Langs => JLangs }
import play.i18n.{ MessagesApi => JMessagesApi }
import play.libs.AnnotationUtils
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Http.{ RequestImpl => JRequestImpl }

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Retains and evaluates what is otherwise expensive reflection work on call by call basis.
 *
 * @param controller The controller to be evaluated
 * @param method     The method to be evaluated
 */
class JavaActionAnnotations(
    val controller: Class[_],
    val method: java.lang.reflect.Method,
    config: ActionCompositionConfiguration
) {
  val parser: Class[_ <: JBodyParser[_]] =
    Seq(
      method.getAnnotation(classOf[play.mvc.BodyParser.Of]),
      controller.getAnnotation(classOf[play.mvc.BodyParser.Of])
    ).filterNot(_ == null)
      .headOption
      .map(_.value)
      .getOrElse(classOf[JBodyParser.Default])

  val controllerAnnotations: Seq[(Annotation, AnnotatedElement)] = play.api.libs.Collections
    .unfoldLeft[Seq[(Annotation, AnnotatedElement)], Option[Class[_]]](Option(controller)) { clazz =>
      clazz.map(c => (Option(c.getSuperclass), c.getDeclaredAnnotations.map((_, c)).toSeq))
    }
    .flatten

  val actionMixins: Seq[(Annotation, Class[_ <: JAction[_]], AnnotatedElement)] = {
    val methodAnnotations = method.getDeclaredAnnotations.map((_, method))
    val allDeclaredAnnotations: Seq[(java.lang.annotation.Annotation, AnnotatedElement)] =
      if (config.controllerAnnotationsFirst) {
        controllerAnnotations ++ methodAnnotations
      } else {
        methodAnnotations ++ controllerAnnotations
      }
    allDeclaredAnnotations
      .collect {
        case (a: play.mvc.With, ae) => a.value.map(c => (a, c, ae)).toSeq
        case (a, ae) if a.annotationType.isAnnotationPresent(classOf[play.mvc.With]) =>
          a.annotationType.getAnnotation(classOf[play.mvc.With]).value.map(c => (a, c, ae)).toSeq
        case (a, ae) if !a.annotationType.isAnnotationPresent(classOf[play.mvc.With]) =>
          AnnotationUtils
            .getIndirectlyPresentAnnotations(a)
            .asScala
            .filter(_.annotationType.isAnnotationPresent(classOf[play.mvc.With]))
            .flatMap(ia => ia.annotationType.getAnnotation(classOf[play.mvc.With]).value.map(c => (ia, c, ae)))
      }
      .flatten
      .map(v => {
        if (v._2.isAnnotationPresent(classOf[javax.inject.Singleton])) {
          // If action singletons would be allowed, it would be very, very likely that concurrent requests interfere with each other
          // when setting the delegate property on that one-and-only singleton instance (see code further below where delegate gets set).
          // If timing is right, it would be possible that, just before calling action.delegate, that the to-be-called delegate was just modified by a concurrent request
          // and points to the next (=delegate) action of that other request (instead of it's own delegate action)
          // As a result (at least) the path/query params of the request would be leaked to the others' request delegate (which eventually will be the action method in the controller).
          // See https://github.com/playframework/playframework/issues/8985#issuecomment-457009162
          throw new RuntimeException(
            s"Singleton action instances are not allowed! Remove the @javax.inject.Singleton annotation from the action class ${v._2.getName}"
          )
        }
        v
      })
      .reverse
  }
}

/*
 * An action that's handling Java requests
 */
abstract class JavaAction(val handlerComponents: JavaHandlerComponents)
    extends Action[play.mvc.Http.RequestBody]
    with JavaHelpers {
  private val logger = Logger(classOf[JAction[_]])

  private def config: ActionCompositionConfiguration = handlerComponents.httpConfiguration.actionComposition

  def invocation(req: JRequest): CompletionStage[JResult]
  val annotations: JavaActionAnnotations

  val executionContext: ExecutionContext = handlerComponents.executionContext

  def apply(req: Request[play.mvc.Http.RequestBody]): Future[Result] = {
    val javaRequest: JRequest = new JRequestImpl(req)

    val rootAction = new JAction[Any] {
      override def call(request: JRequest): CompletionStage[JResult] = invocation(request)
    }

    val baseAction = handlerComponents.actionCreator.createAction(javaRequest, annotations.method)

    val endOfChainAction = if (config.executeActionCreatorActionFirst) {
      rootAction
    } else {
      rootAction.precursor = baseAction
      baseAction.delegate = rootAction
      baseAction
    }

    val firstUserDeclaredAction = annotations.actionMixins.foldLeft[JAction[_ <: Any]](endOfChainAction) {
      case (delegate, (annotation, actionClass, annotatedElement)) =>
        val action = handlerComponents.getAction(actionClass).asInstanceOf[play.mvc.Action[Object]]
        action.configuration = annotation
        delegate.precursor = action
        action.delegate = delegate
        action.annotatedElement = annotatedElement
        action
    }

    val firstAction = if (config.executeActionCreatorActionFirst) {
      firstUserDeclaredAction.precursor = baseAction
      baseAction.delegate = firstUserDeclaredAction
      baseAction
    } else {
      firstUserDeclaredAction
    }

    val trampolineWithContext: ExecutionContext = {
      val javaClassLoader = Thread.currentThread.getContextClassLoader
      new HttpExecutionContext(javaClassLoader, trampoline)
    }
    if (logger.isDebugEnabled) {
      val actionChain = play.api.libs.Collections
        .unfoldLeft[JAction[_], Option[JAction[_]]](Option(firstAction)) { action =>
          action.map(a => (Option(a.delegate), a))
        }
        .reverse
      logger.debug("### Start of action order")
      actionChain
        .zip(Stream.from(1))
        .foreach({
          case (action, index) =>
            logger.debug(
              s"${index}. ${action.getClass.getName}" +
                (if (action.annotatedElement != null) {
                   s" defined on ${action.annotatedElement}"
                 })
            )
        })
      logger.debug("### End of action order")
    }
    val actionFuture: Future[Future[JResult]] = Future {
      FutureConverters.toScala(firstAction.call(javaRequest))
    }(trampolineWithContext)
    val flattenedActionFuture: Future[JResult] = actionFuture.flatMap(identity)(trampoline)
    val resultFuture: Future[Result]           = flattenedActionFuture.map(_.asScala)(trampoline)
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

/**
 * Group components that are commonly to serve requests.
 */
@deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
trait JavaContextComponents {
  def messagesApi: JMessagesApi
  def langs: JLangs
  def fileMimeTypes: FileMimeTypes
  def httpConfiguration: HttpConfiguration
}

@deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
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
  @deprecated(
    "Use the corresponding methods that provide MessagesApi, Langs, FileMimeTypes or HttpConfiguration",
    "2.8.0"
  )
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
    @deprecated("Inject MessagesApi, Langs, FileMimeTypes or HttpConfiguration instead", "2.8.0")
    val contextComponents: JavaContextComponents
) extends JavaHandlerComponents {
  def getBodyParser[A <: JBodyParser[_]](parserClass: Class[A]): A = injector.instanceOf(parserClass)
  def getAction[A <: JAction[_]](actionClass: Class[A]): A         = injector.instanceOf(actionClass)
}
