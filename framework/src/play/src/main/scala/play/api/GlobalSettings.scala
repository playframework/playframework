/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import play.api.mvc._
import play.core.j

import scala.concurrent.Future
import play.api.http._
import play.api.http.Status._

/**
 * Defines an application’s global settings.
 *
 * To define your own global settings, just create a `Global` object in the `_root_` package.
 * {{{
 * object Global extends GlobalSettings {
 *
 *   override def onStart(app: Application) {
 *     Logger.info("Application is started!!!")
 *   }
 *
 * }
 * }}}
 */
@deprecated("Use dependency injection", "2.5.0")
trait GlobalSettings {

  private val dhehCache = Application.instanceCache[DefaultHttpErrorHandler]
  /**
   * Note, this should only be used for the default implementations of onError, onHandlerNotFound and onBadRequest.
   */
  private def defaultErrorHandler: HttpErrorHandler = {
    Play.privateMaybeApplication.fold[HttpErrorHandler](DefaultHttpErrorHandler)(dhehCache)
  }

  /**
   * This should be used for all invocations of error handling in Global.
   */
  private def configuredErrorHandler: HttpErrorHandler = {
    Play.privateMaybeApplication.fold[HttpErrorHandler](DefaultHttpErrorHandler)(_.errorHandler)
  }

  private val jchrhCache = Application.instanceCache[JavaCompatibleHttpRequestHandler]
  private def defaultRequestHandler: Option[DefaultHttpRequestHandler] = {
    Play.privateMaybeApplication.map(jchrhCache)
  }

  private val httpFiltersCache = Application.instanceCache[HttpFilters]
  private def filters: HttpFilters = {
    Play.privateMaybeApplication.fold[HttpFilters](NoHttpFilters)(httpFiltersCache)
  }

  /**
   * Called before the application starts.
   *
   * Resources managed by plugins, such as database connections, are likely not available at this point.
   *
   * @param app the application
   */
  def beforeStart(app: Application) {
  }

  /**
   * Called once the application is started.
   *
   * @param app the application
   */
  def onStart(app: Application) {
  }

  /**
   * Called on application stop.
   *
   * @param app the application
   */
  def onStop(app: Application) {
  }

  /**
   * Retrieve the (RequestHeader,Handler) to use to serve this request.
   * Default is: route, tag request, then apply filters
   */
  def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {
    def notFoundHandler = Action.async(BodyParsers.parse.empty)(req =>
      configuredErrorHandler.onClientError(req, NOT_FOUND)
    )

    val (routedRequest, handler) = onRouteRequest(request) map {
      case handler: RequestTaggingHandler => (handler.tagRequest(request), handler)
      case otherHandler => (request, otherHandler)
    } getOrElse {

      // We automatically permit HEAD requests against any GETs without the need to
      // add an explicit mapping in Routes
      request.method match {
        case HttpVerbs.HEAD =>
          onRouteRequest(request.copy(method = HttpVerbs.GET)) match {
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

    (routedRequest, doFilter(rh => handler)(routedRequest))
  }

  val httpConfigurationCache = Application.instanceCache[HttpConfiguration]
  /**
   * Filters.
   */
  def doFilter(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      val context = Play.privateMaybeApplication.fold("") { app =>
        httpConfigurationCache(app).context.stripSuffix("/")
      }
      val inContext = context.isEmpty || request.path == context || request.path.startsWith(context + "/")
      next(request) match {
        case action: EssentialAction if inContext => doFilter(action)
        case handler => handler
      }
  }

  /**
   * Filters for EssentialAction.
   */
  def doFilter(next: EssentialAction): EssentialAction = {
    filters.filters.foldRight(next)(_ apply _)
  }

  /**
   * Called when an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * @param request the HTTP request header (the body has not been parsed yet)
   * @return an action to handle this request - if no action is returned, a 404 not found result will be sent to client
   * @see onHandlerNotFound
   */
  def onRouteRequest(request: RequestHeader): Option[Handler] = defaultRequestHandler.flatMap { handler =>
    handler.routeRequest(request)
  }

  /**
   * Called when an exception occurred.
   *
   * The default is to send the framework default error page.
   *
   * @param request The HTTP request header
   * @param ex The exception
   * @return The result to send to the client
   */
  def onError(request: RequestHeader, ex: Throwable): Future[Result] =
    defaultErrorHandler.onServerError(request, ex)

  /**
   * Called when no action was found to serve a request.
   *
   * The default is to send the framework default 404 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onHandlerNotFound(request: RequestHeader): Future[Result] =
    defaultErrorHandler.onClientError(request, play.api.http.Status.NOT_FOUND)

  /**
   * Called when an action has been found, but the request parsing has failed.
   *
   * The default is to send the framework default 400 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onBadRequest(request: RequestHeader, error: String): Future[Result] =
    defaultErrorHandler.onClientError(request, play.api.http.Status.BAD_REQUEST, error)
}

/**
 * The default global settings if not defined in the application.
 */
object DefaultGlobal extends GlobalSettings.Deprecated

object GlobalSettings {

  type Deprecated = GlobalSettings

  /**
   * Load the global object.
   *
   * @param configuration The configuration to read the loading from.
   * @param environment The environment to load the global object from.
   * @return
   */
  @deprecated("Use dependency injection", "2.5.0")
  def apply(configuration: Configuration, environment: Environment): GlobalSettings.Deprecated = {
    val globalClass = configuration.getString("application.global").getOrElse("Global")

    def javaGlobal: Option[play.GlobalSettings] = try {
      Option(environment.classLoader.loadClass(globalClass).newInstance().asInstanceOf[play.GlobalSettings])
    } catch {
      case e: InstantiationException => None
      case e: ClassNotFoundException => None
    }

    def scalaGlobal: GlobalSettings = try {
      environment.classLoader.loadClass(globalClass + "$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
    } catch {
      case e: ClassNotFoundException if !configuration.getString("application.global").isDefined =>
        DefaultGlobal
      case e if configuration.getString("application.global").isDefined => {
        throw configuration.reportError("application.global",
          s"Cannot initialize the custom Global object ($globalClass) (perhaps it's a wrong reference?)", Some(e))
      }
    }

    try {
      javaGlobal.map(new j.JavaGlobalSettingsAdapter(_)).getOrElse(scalaGlobal)
    } catch {
      case e: PlayException => throw e
      case e: ThreadDeath => throw e
      case e: VirtualMachineError => throw e
      case e: Throwable => throw new PlayException(
        "Cannot init the Global object",
        e.getMessage,
        e
      )
    }
  }

}
