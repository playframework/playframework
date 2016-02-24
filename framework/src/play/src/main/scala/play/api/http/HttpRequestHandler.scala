/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject.{ Inject, Provider }

import play.api.http.Status._
import play.api.inject.{ Binding, BindingKey }
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router
import play.api.{ Configuration, Environment, GlobalSettings, PlayConfig }
import play.core.j.{ JavaHttpRequestHandlerDelegate, JavaHandler, JavaHandlerComponents }
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

  /**
   * Adapt this to a Java HttpRequestHandler
   */
  def asJava = new JavaHttpRequestHandlerDelegate(this)
}

object HttpRequestHandler {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    val fromConfiguration = Reflect.bindingsFromConfiguration[HttpRequestHandler, play.http.HttpRequestHandler, play.core.j.JavaHttpRequestHandlerAdapter, play.http.DefaultHttpRequestHandler, JavaCompatibleHttpRequestHandler](environment,
      PlayConfig(configuration), "play.http.requestHandler", "RequestHandler")

    val javaComponentsBindings = Seq(BindingKey(classOf[play.core.j.JavaHandlerComponents]).to[play.core.j.DefaultJavaHandlerComponents])

    fromConfiguration ++ javaComponentsBindings
  }
}

object ActionCreator {
  import play.http.{ ActionCreator, HttpRequestHandlerActionCreator }

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.configuredClass[ActionCreator, ActionCreator, HttpRequestHandlerActionCreator](environment,
      PlayConfig(configuration), "play.http.actionCreator", "ActionCreator").fold(Seq[Binding[_]]()) { either =>
        val impl = either.fold(identity, identity)
        Seq(BindingKey(classOf[ActionCreator]).to(impl))
      }
  }
}

/**
 * Implementation of a [HttpRequestHandler] that always returns NotImplemented results
 */
object NotImplementedHttpRequestHandler extends HttpRequestHandler {
  def handlerForRequest(request: RequestHeader) = request -> EssentialAction(_ => Accumulator.done(Results.NotImplemented))
}

/**
 * A base implementation of the [[HttpRequestHandler]] that handles Scala actions. If you use Java actions in your
 * application, you should override [[JavaCompatibleHttpRequestHandler]]; otherwise you can override this for your
 * custom handler.
 *
 * Technically, this is not the default request handler that Play uses, rather, the [[JavaCompatibleHttpRequestHandler]]
 * is the default one, in order to provide support for Java actions.
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

  private val context = configuration.context.stripSuffix("/")

  private def inContext(path: String): Boolean = {
    // Assume context is a string without a trailing '/'.
    // Handle four cases:
    // * context.isEmpty
    //   - There is no context, everything is in context, short circuit all other checks
    // * !path.startsWith(context)
    //   - Either path is shorter than context or starts with a different prefix.
    // * path.startsWith(context) && path.length == context.length
    //   - Path is equal to context.
    // * path.startsWith(context) && path.charAt(context.length) == '/')
    //   - Path starts with context followed by a '/' character.
    context.isEmpty ||
      (path.startsWith(context) && (path.length == context.length || path.charAt(context.length) == '/'))
  }

  def handlerForRequest(request: RequestHeader) = {

    def notFoundHandler = Action.async(BodyParsers.parse.empty)(req =>
      errorHandler.onClientError(req, NOT_FOUND)
    )

    val (routedRequest, handler) = routeRequest(request) map {
      case handler: RequestTaggingHandler => (handler.tagRequest(request), handler)
      case otherHandler => (request, otherHandler)
    } getOrElse {

      // We automatically permit HEAD requests against any GETs without the need to
      // add an explicit mapping in Routes
      request.method match {
        case HttpVerbs.HEAD =>
          routeRequest(request.copy(method = HttpVerbs.GET)) match {
            case Some(action: EssentialAction) => action match {
              case handler: RequestTaggingHandler => (handler.tagRequest(request), action)
              case _ => (request, action)
            }
            case None => (request, notFoundHandler)
          }
        case _ =>
          (request, notFoundHandler)
      }
    }

    (routedRequest, filterHandler(rh => handler)(routedRequest))
  }

  /**
   * Apply any filters to the given handler.
   */
  protected def filterHandler(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      next(request) match {
        case action: EssentialAction if inContext(request.path) => filterAction(action)
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
 * This handler should be used in order to support legacy global settings request interception.
 *
 * Custom handlers need not extend this.
 */
@deprecated("GlobalSettings is deprecated. Use DefaultHttpRequestHandler or JavaCompatibleHttpRequestHandler.", "2.5.0")
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
