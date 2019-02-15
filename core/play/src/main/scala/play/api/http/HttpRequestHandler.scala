/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import javax.inject.Inject

import play.api.ApplicationLoader.DevContext
import play.api.http.Status._
import play.api.inject.{ Binding, BindingKey }
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router
import play.api.{ Configuration, Environment, OptionalDevContext }
import play.core.j.{ JavaHandler, JavaHandlerComponents, JavaHttpRequestHandlerDelegate }
import play.core.{ DefaultWebCommands, WebCommands }
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

    Reflect.bindingsFromConfiguration[HttpRequestHandler, play.http.HttpRequestHandler, play.core.j.JavaHttpRequestHandlerAdapter, play.http.DefaultHttpRequestHandler, JavaCompatibleHttpRequestHandler](
      environment,
      configuration, "play.http.requestHandler", "RequestHandler")
  }
}

object ActionCreator {
  import play.http.{ ActionCreator, DefaultActionCreator }

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.configuredClass[ActionCreator, ActionCreator, DefaultActionCreator](
      environment,
      configuration, "play.http.actionCreator", "ActionCreator").fold(Seq[Binding[_]]()) { either =>
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
 */
class DefaultHttpRequestHandler(
    webCommands: WebCommands,
    optDevContext: Option[DevContext],
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter]) extends HttpRequestHandler {

  @Inject
  def this(
    webCommands: WebCommands,
    optDevContext: OptionalDevContext,
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: HttpFilters) = {
    this(webCommands, optDevContext.devContext, router, errorHandler, configuration, filters.filters)
  }

  @deprecated("Use the main DefaultHttpRequestHandler constructor", "2.7.0")
  def this(router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: HttpFilters) = {
    this(new DefaultWebCommands, None, router, errorHandler, configuration, filters.filters)
  }

  @deprecated("Use the main DefaultHttpRequestHandler constructor", "2.7.0")
  def this(router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: EssentialFilter*) = {
    this(new DefaultWebCommands, None, router, errorHandler, configuration, filters)
  }

  private val context = configuration.context.stripSuffix("/")

  /** Work out whether a path is handled by this application. */
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

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) = {

    def handleWithStatus(status: Int) = ActionBuilder.ignoringBody.async(BodyParsers.utils.empty)(req =>
      errorHandler.onClientError(req, status)
    )

    /**
     * Call the router to get the handler, but with a couple of types of fallback.
     * First, if a HEAD request isn't explicitly routed try routing it as a GET
     * request. Second, if no routing information is present, fall back to a 404
     * error.
     */
    def routeWithFallback(request: RequestHeader): Handler = {
      routeRequest(request).getOrElse {
        request.method match {
          // We automatically permit HEAD requests against any GETs without the need to
          // add an explicit mapping in Routes. Since we couldn't route the HEAD request,
          // try to get a Handler for the equivalent GET request instead. Notes:
          // 1. The handler returned will still be passed a HEAD request when it is
          //    actually evaluated.
          // 2. When the endpoint is to a WebSocket connection, the handler returned
          //    will result in a Bad Request. That is because, while we can translate
          //    GET requests to HEAD, we can't do that for WebSockets, since there is
          //    no way (or reason) to Upgrade the connection. For more information see
          //    https://tools.ietf.org/html/rfc6455#section-1.3
          case HttpVerbs.HEAD => {
            routeRequest(request.withMethod(HttpVerbs.GET)) match {
              case Some(handler: Handler) => handler match {
                case ws: WebSocket => handleWithStatus(BAD_REQUEST)
                case _ => handler
              }
              case None => handleWithStatus(NOT_FOUND)
            }
          }
          case _ =>
            // An Action for a 404 error
            handleWithStatus(NOT_FOUND)
        }
      }
    }

    // If we've got a BuildLink (i.e. if we're running in dev mode) then run the WebCommands.
    // The WebCommands will have a chance to intercept the request and override the result.
    // This is used by, for example, the evolutions code to present an evolutions UI to the
    // user when the access the web page through a browser.
    //
    // In prod mode this code will not be run.
    val webCommandResult: Option[Result] = optDevContext.flatMap { devContext: DevContext =>
      webCommands.handleWebCommand(request, devContext.buildLink, devContext.buildLink.projectPath)
    }

    // Look at the result of the WebCommand and either short-circuit the result or apply
    // the routes, filters, actions, etc.
    webCommandResult match {
      case Some(r) =>
        // A WebCommand returned a result
        (request, ActionBuilder.ignoringBody { r })
      case None =>
        // 1. Query the router to get a handler
        // 2. Resolve handlers that preprocess the request
        // 3. Modify the handler to do filtering, if necessary
        // 4. Again resolve any handlers that do preprocessing
        val routedHandler = routeWithFallback(request)
        val (preprocessedRequest, preprocessedHandler) = Handler.applyStages(request, routedHandler)
        val filteredHandler = filterHandler(preprocessedRequest, preprocessedHandler)
        val (preprocessedPreprocessedRequest, preprocessedFilteredHandler) = Handler.applyStages(preprocessedRequest, filteredHandler)
        (preprocessedPreprocessedRequest, preprocessedFilteredHandler)
    }
  }

  /**
   * Apply any filters to the given handler.
   */
  @deprecated("Use filterHandler(RequestHeader, Handler) instead", "2.6.0")
  protected def filterHandler(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      next(request) match {
        case action: EssentialAction if inContext(request.path) => filterAction(action)
        case handler => handler
      }
  }

  /**
   * Update the given handler so that when the handler is run any filters will also be run. The
   * default behavior is to wrap all [[play.api.mvc.EssentialAction]]s by calling `filterAction`, but to leave
   * other kinds of handlers unchanged.
   */
  protected def filterHandler(request: RequestHeader, handler: Handler): Handler = {
    handler match {
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
 * A Java compatible HTTP request handler.
 *
 * If a router routes to Java actions, it will return instances of [[play.core.j.JavaHandler]].  This takes an instance
 * of [[play.core.j.JavaHandlerComponents]] to supply the necessary infrastructure to invoke a Java action, and returns
 * a new [[play.api.mvc.Handler]] that the core of Play knows how to handle.
 *
 * If your application routes to Java actions, then you must use this request handler as the base class as is or as
 * the base class for your custom [[HttpRequestHandler]].
 */
class JavaCompatibleHttpRequestHandler(
    webCommands: WebCommands,
    optDevContext: Option[DevContext],
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter],
    handlerComponents: JavaHandlerComponents)
  extends DefaultHttpRequestHandler(webCommands, optDevContext, router, errorHandler, configuration, filters) {

  @Inject
  def this(
    webCommands: WebCommands,
    optDevContext: OptionalDevContext,
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: HttpFilters,
    handlerComponents: JavaHandlerComponents) = {
    this(webCommands, optDevContext.devContext, router, errorHandler, configuration, filters.filters, handlerComponents)
  }

  @deprecated("Use the main JavaCompatibleHttpRequestHandler constructor", "2.7.0")
  def this(router: Router, errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration, filters: HttpFilters, handlerComponents: JavaHandlerComponents) = {
    this(new DefaultWebCommands, new OptionalDevContext(None), router, errorHandler, configuration, filters, handlerComponents)
  }

  // This is a Handler that, when evaluated, converts its underlying JavaHandler into
  // another handler.
  private class MapJavaHandler(nextHandler: Handler) extends Handler.Stage {
    override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = {
      // First, preprocess the request and our handler so we can get the underlying handler
      val (preprocessedRequest, preprocessedHandler) = Handler.applyStages(requestHeader, nextHandler)

      // Next, if the underlying handler is a JavaHandler, get its real handler
      val mappedHandler: Handler = preprocessedHandler match {
        case javaHandler: JavaHandler => javaHandler.withComponents(handlerComponents)
        case other => other
      }

      (preprocessedRequest, mappedHandler)
    }
  }

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    // Override the usual routing logic so that any JavaHandlers are
    // rewritten.
    super.routeRequest(request).map(new MapJavaHandler(_))
  }
}
