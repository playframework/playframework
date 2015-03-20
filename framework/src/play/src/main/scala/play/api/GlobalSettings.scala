/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import javax.inject.{ Inject, Singleton }

import play.api.mvc._
import java.io.File
import play.core.j

import scala.concurrent.Future
import play.api.http._
import play.core.actions.HeadAction
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
trait GlobalSettings {

  private val dhehCache = Application.instanceCache[DefaultHttpErrorHandler]
  /**
   * Note, this should only be used for the default implementations of onError, onHandlerNotFound and onBadRequest.
   */
  private def defaultErrorHandler: HttpErrorHandler = {
    Play.maybeApplication.fold[HttpErrorHandler](DefaultHttpErrorHandler)(dhehCache)
  }

  /**
   * This should be used for all invocations of error handling in Global.
   */
  private def configuredErrorHandler: HttpErrorHandler = {
    Play.maybeApplication.fold[HttpErrorHandler](DefaultHttpErrorHandler)(_.errorHandler)
  }

  private val jchrhCache = Application.instanceCache[JavaCompatibleHttpRequestHandler]
  private def defaultRequestHandler: Option[DefaultHttpRequestHandler] = {
    Play.maybeApplication.map(jchrhCache)
  }

  private val httpFiltersCache = Application.instanceCache[HttpFilters]
  private def filters: HttpFilters = {
    Play.maybeApplication.fold[HttpFilters](NoHttpFilters)(httpFiltersCache)
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
   * Additional configuration provided by the application.  This is invoked by the default implementation of
   * onLoadConfig, so if you override that, this won't be invoked.
   */
  def configuration: Configuration = Configuration.empty

  /**
   * Called just after configuration has been loaded, to give the application an opportunity to modify it.
   *
   * @param config the loaded configuration
   * @param path the application path
   * @param classloader The applications classloader
   * @param mode The mode the application is running in
   * @return The configuration that the application should use
   */
  def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration =
    config ++ configuration

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
      val missingHandler: Handler = request.method match {
        case HttpVerbs.HEAD =>
          val headAction = onRouteRequest(request.copy(method = HttpVerbs.GET)) match {
            case Some(action: EssentialAction) => action
            case None => notFoundHandler
          }
          new HeadAction(headAction)
        case _ =>
          notFoundHandler
      }
      (request, missingHandler)
    }

    (routedRequest, doFilter(rh => handler)(routedRequest))
  }

  val httpConfigurationCache = Application.instanceCache[HttpConfiguration]
  /**
   * Filters.
   */
  def doFilter(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      val context = Play.maybeApplication.fold("/") { app =>
        httpConfigurationCache(app).context.replaceAll("/$", "") + "/"
      }
      next(request) match {
        case action: EssentialAction if request.path startsWith context => doFilter(action)
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

  @deprecated("onRequestCompletion is no longer invoked by Play. The same functionality can be achieved by adding a filter that attaches a onDoneEnumerating callback onto the returned Result Enumerator.", "2.4.0")
  def onRequestCompletion(request: RequestHeader) {
  }

}

/**
 * The default global settings if not defined in the application.
 */
object DefaultGlobal extends GlobalSettings

object GlobalSettings {

  /**
   * Load the global object.
   *
   * @param configuration The configuration to read the loading from.
   * @param environment The environment to load the global object from.
   * @return
   */
  def apply(configuration: Configuration, environment: Environment): GlobalSettings = {
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
      case e: ClassNotFoundException if !configuration.getString("application.global").isDefined => DefaultGlobal
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

/**
 * The Global plugin executes application's `globalSettings` `onStart` and `onStop`.
 */
@Singleton
class GlobalPlugin @Inject() (app: Application) extends Plugin.Deprecated {

  // Call before start now
  app.global.beforeStart(app)

  /**
   * Called when the application starts.
   */
  override def onStart() {
    app.global.onStart(app)
  }

  /**
   * Called when the application stops.
   */
  override def onStop() {
    app.global.onStop(app)
  }

}
