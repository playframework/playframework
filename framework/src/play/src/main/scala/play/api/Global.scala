package play.api

import play.api.mvc._

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

  import Results._

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
   * Additional configuration provided by the application.
   */
  def configuration: Configuration = Configuration.empty

  /**
   * Called when an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * @param request the HTTP request header (the body has not been parsed yet)
   * @return an action to handle this request - if no action is returned, a 404 not found result will be sent to client
   * @see onActionNotFound
   */
  def onRouteRequest(request: RequestHeader): Option[Handler] = Play.maybeApplication.flatMap(_.routes.flatMap { router =>
    router.handlerFor(request)
  })

  /**
   * Called when an exception occurred.
   *
   * The default is to send the framework default error page.
   *
   * @param request The HTTP request header
   * @param ex The exception
   * @return The result to send to the client
   */
  def onError(request: RequestHeader, ex: Throwable): Result = {
    InternalServerError(Play.maybeApplication.map {
      case app if app.mode == Mode.Dev => views.html.defaultpages.devError.f
      case app => views.html.defaultpages.error.f
    }.getOrElse(views.html.defaultpages.devError.f) {
      ex match {
        case e: PlayException.UsefulException => e
        case e => UnexpectedException(unexpected = Some(e))
      }
    })
  }

  /**
   * Called when no action was found to serve a request.
   *
   * The default is to send the framework default 404 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound(Play.maybeApplication.map {
      case app if app.mode == Mode.Dev => views.html.defaultpages.devNotFound.f
      case app => views.html.defaultpages.notFound.f
    }.getOrElse(views.html.defaultpages.devNotFound.f)(request, Play.maybeApplication.flatMap(_.routes)))
  }

  /**
   * Called when an action has been found, but the request parsing has failed.
   *
   * The default is to send the framework default 400 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onBadRequest(request: RequestHeader, error: String): Result = {
    BadRequest(views.html.defaultpages.badRequest(request, error))
  }

}

/**
 * The default global settings if not defined in the application.
 */
object DefaultGlobal extends GlobalSettings

/**
 * The Global plugin executes application's `globalSettings` `onStart` and `onStop`.
 */
class GlobalPlugin(app: Application) extends Plugin {

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