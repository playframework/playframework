/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.system

import play.api.{ DefaultGlobal, Play, Application }
import play.api.mvc.{ SimpleResult, RequestHeader, Handler }
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * The context for a handler executor to run.
 */
trait HandlerExecutorContext[BackendRequest] {
  /**
   * The application, if there is one.  There may not be an application if the application failed to compile or start.
   */
  def application: Option[Application]

  /**
   * The handler executors
   */
  def handlerExecutors: Seq[HandlerExecutor[BackendRequest]]

  /**
   * Execute the given handler with the handlers
   */
  def apply(request: RequestHeader, backend: BackendRequest, handler: Handler): Option[Future[_]] = {
    HandlerExecutor.collectFirst(handlerExecutors, this, request, backend, handler)
  }

  /**
   * Handle an error.  This will return a future that can be returned from HandlerExecutor.apply.
   */
  def handleError(request: RequestHeader, backend: BackendRequest, error: Throwable): Option[Future[_]] = {
    sendResult(request, backend,
      application.map(_.handleError(request, error)).getOrElse(DefaultGlobal.onError(request, error))
    )
  }

  /**
   * Send an error result.  This will return a future that can be returned from HandlerExecutor.apply.
   */
  def sendResult(request: RequestHeader, backend: BackendRequest, result: Future[SimpleResult]): Option[Future[_]]
}

/**
 * Executes handlers on the given backend.
 *
 * This API is experimental, and apart from the built in handlers, is not officially supported.  It is provided as a
 * means to allow experimentation and innovation with new standards and technologies.  Use at your own risk.
 *
 * @tparam BackendRequest A backend specific request object, contains everything required to handle the specific
 *                        request.
 */
trait HandlerExecutor[BackendRequest] {

  /**
   * Maybe handle the given backend request using the given handler.
   *
   * @param context The context.
   * @param request The Play request header.
   * @param backend The backend request.
   * @param handler The handler to execute, if this executor supports that handler.
   * @return A future that is redeemed when the request has been processed, or None if this executor doesn't know how
   *         to execute the given handler.
   */
  def apply(context: HandlerExecutorContext[BackendRequest], request: RequestHeader, backend: BackendRequest, handler: Handler): Option[Future[_]]
}

object HandlerExecutor {

  /**
   * Collect the result from the first HandlerExecutor that returns a result.
   */
  def collectFirst[B](handlerExecutors: Seq[HandlerExecutor[B]],
    context: HandlerExecutorContext[B],
    request: RequestHeader,
    backend: B,
    handler: Handler): Option[Future[_]] = {

    // Can't use Seq.collectFirst.  It only accepts partial functions, and calls isDefinedAt, which means the
    // executor will get called twice if we wrap it in a partial function.
    for (he <- handlerExecutors.iterator) {
      val result = he(context, request, backend, handler)
      if (result.isDefined) {
        return result
      }
    }
    None
  }

  private[play] def discoverHandlerExecutors[B](classloader: ClassLoader, filename: String,
    builtins: Seq[HandlerExecutor[B]])(implicit ct: ClassTag[B]): Seq[HandlerExecutor[B]] = {
    import scala.collection.JavaConversions._
    import scalax.io.JavaConverters._
    classloader.getResources(filename).toList
      .flatMap(_.asInput.lines())
      .map(_.trim())
      .filterNot(_.isEmpty)
      .distinct
      .flatMap { className =>
        try {
          val clazz = classloader.loadClass(className)
          val plugin = (className match {
            case obj if obj.endsWith("$") => clazz.getField("MODULE$").get(null)
            case _ => clazz.newInstance()
          }).asInstanceOf[HandlerExecutor[B]]
          Seq(plugin)
        } catch {
          case e: Exception =>
            Play.logger.warn("Error loading handler executor, ignoring: " + className, e)
            Nil
        }
      } ++ builtins
  }

}