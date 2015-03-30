/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.http

import javax.inject.{ Provider, Inject }

import play.api.inject.{ BindingKey, Binding }
import play.api.libs.iteratee.Done
import play.api.{ PlayConfig, Configuration, Environment, GlobalSettings }
import play.api.http.Status._
import play.api.mvc._
import play.api.routing.Router
import play.core.actions.HeadAction
import play.core.j.{ JavaHandler, JavaHandlerComponents }
import play.utils.Reflect

/**
 * Primary entry point for all HTTP requests on Play applications.
 */
trait HttpRequestHandler {

  /**
   * Get a handler for the given request.
   *
   * In addition to retrieving a handler for the request, the request itself may be modified - typically it will be
   * tagged with routing information.  It is also acceptable to simply return the request as is.  Play will switch to
   * using the returned request from this point in in its request handling.
   *
   * The reason why the API allows returning a modified request, rather than just wrapping the Handler in a new Handler
   * that modifies the request, is so that Play can pass this request to other handlers, such as error handlers, or
   * filters, and they will get the tagged/modified request.
   *
   * @param request The request to handle
   * @return The possibly modified/tagged request, and a handler to handle it
   */
  def handlerForRequest(request: RequestHeader): (RequestHeader, Handler)
}

object HttpRequestHandler {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    val javaComponentsBinding = BindingKey(classOf[play.core.j.JavaHandlerComponents]).toSelf

    Reflect.configuredClass[HttpRequestHandler, play.http.HttpRequestHandler, GlobalSettingsHttpRequestHandler](environment,
      PlayConfig(configuration), "play.http.requestHandler", "RequestHandler") match {
        case None => Nil
        case Some(Left(scalaImpl)) =>
          Seq(
            BindingKey(classOf[HttpRequestHandler]).to(scalaImpl),
            // Need to bind the default Java one in case the Scala one depends on JavaHandlerComponents
            BindingKey(classOf[play.http.HttpRequestHandler]).to[play.http.GlobalSettingsHttpRequestHandler],
            javaComponentsBinding
          )
        case Some(Right(javaImpl)) =>
          Seq(
            BindingKey(classOf[HttpRequestHandler]).to[JavaCompatibleHttpRequestHandler],
            BindingKey(classOf[play.http.HttpRequestHandler]).to(javaImpl),
            javaComponentsBinding
          )
      }
  }
}

/**
 * Implementation of a [HttpRequestHandler] that always returns NotImplemented results
 */
object NotImplementedHttpRequestHandler extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = request -> EssentialAction(_ => Done(Results.NotImplemented))
}

/**
 * A default implementation of the [[HttpRequestHandler]].
 *
 * This can be conveniently overridden to plug in global interception or custom routing logic into Play's existing
 * request handling infrastructure.
 *
 * Technically, this is not the default request handler that Play uses, rather, the [[GlobalSettingsHttpRequestHandler]]
 * is the default one, in order to ensure that existing legacy implementations of global request interception still
 * work. In future, this will become the default request handler.
 *
 * The default implementations of method interception methods on [[play.api.GlobalSettings]] match the implementations
 * of this, so when not providing any custom logic, whether this is used or the global settings http request handler
 * is used is irrelevant.
 */
class DefaultHttpRequestHandler(router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration,
    filters: EssentialFilter*) extends HttpRequestHandler {

  @Inject
  def this(router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: HttpFilters) =
    this(router, errorHandler, configuration, filters.filters: _*)

  private val context = if (configuration.context.endsWith("/")) {
    configuration.context
  } else {
    configuration.context + "/"
  }

  def handlerForRequest(request: RequestHeader) = {

    def notFoundHandler = Action.async(BodyParsers.parse.empty)(req =>
      errorHandler.onClientError(request, NOT_FOUND)
    )

    val (routedRequest, handler) = routeRequest(request) map {
      case handler: RequestTaggingHandler => (handler.tagRequest(request), handler)
      case otherHandler => (request, otherHandler)
    } getOrElse {

      // We automatically permit HEAD requests against any GETs without the need to
      // add an explicit mapping in Routes
      val missingHandler: Handler = request.method match {
        case HttpVerbs.HEAD =>
          val headAction = routeRequest(request.copy(method = HttpVerbs.GET)) match {
            case Some(action: EssentialAction) => action
            case _ => notFoundHandler
          }
          new HeadAction(headAction)
        case _ =>
          notFoundHandler
      }
      (request, missingHandler)
    }

    (routedRequest, filterHandler(rh => handler)(routedRequest))
  }

  /**
   * Apply any filters to the given handler.
   */
  protected def filterHandler(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      next(request) match {
        case action: EssentialAction if request.path startsWith context => filterAction(action)
        case handler => handler
      }
  }

  /**
   * Apply filters to the given action.
   */
  protected def filterAction(next: EssentialAction): EssentialAction = {
    filters.foldRight(next)(_ apply _)
  }

  /**
   * Called when an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * This method can be overridden if you want to provide some custom routing strategies, for example, using different
   * routers based on various request parameters.
   *
   * @param request The request
   * @return A handler to handle the request, if one can be found
   */
  def routeRequest(request: RequestHeader): Option[Handler] = {
    router.handlerFor(request)
  }

}

/**
 * An [[HttpRequestHandler]] that delegates to [[play.api.GlobalSettings]].
 *
 * This is the default request handler used by Play, in order to support legacy global settings request interception.
 *
 * Custom handlers need not extend this.
 */
class GlobalSettingsHttpRequestHandler @Inject() (global: Provider[GlobalSettings]) extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = global.get.onRequestReceived(request)
}

/**
 * A Java compatible HTTP request handler.
 *
 * If a router routes to Java actions, it will return instances of [[play.core.j.JavaHandler]].  This takes an instance
 * of [[play.core.j.JavaHandlerComponents]] to supply the necessary infrastructure to invoke a Java action, and returns
 * a new [[play.api.mvc.Handler]] that the core of Play knows how to handle.
 *
 * If your application routes to Java actions, then you must use this request handler as the base class as is or as
 * the base class for your custom [[HttpRequestHandler]].
 */
class JavaCompatibleHttpRequestHandler @Inject() (router: Router, errorHandler: HttpErrorHandler,
  configuration: HttpConfiguration, filters: HttpFilters, components: JavaHandlerComponents) extends DefaultHttpRequestHandler(router,
  errorHandler, configuration, filters.filters: _*) {

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    super.routeRequest(request) match {
      case Some(javaHandler: JavaHandler) =>
        Some(javaHandler.withComponents(components))
      case other => other
    }
  }
}
