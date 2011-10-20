package play.api

import play.api.mvc._

/**
 * Define the global settings for an application.
 * To define your own global settings, just create a Global object in the _root_ package.
 *
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
   * Called before the application start.
   * Resources managed by plugins like database connections are likely not available at this point.
   *
   * @param app The application.
   */
  def beforeStart(app: Application) {
  }

  /**
   * Called once the application is started.
   *
   * @param app The application.
   */
  def onStart(app: Application) {
  }

  /**
   * Called on application stop.
   *
   * @param app The application.
   */
  def onStop(app: Application) {
  }

  /**
   * Called on an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * @param request The HTTP request header (body has not been parsed yet).
   * @return Maybe an action to handle this request (if no action is returned, a 404 not found result will be sent to client).
   * @see onActionNotFound
   */
  def onRouteRequest(request: RequestHeader): Option[Action[_]] = Play._currentApp.routes.flatMap { router =>
    router.actionFor(request)
  }

  /**
   * Called when an exception occured.
   *
   * The default is to send the framework default error page.
   *
   * @param request The HTTP request header.
   * @param ex The exception.
   * @return The result to send to the client.
   */
  def onError(request: RequestHeader, ex: Throwable): Result = {

    Logger.error(
      """
      |
      |! %sInternal server error, for request [%s] ->
      |""".stripMargin.format(ex match {
        case p: PlayException => "@" + p.id + " - "
        case _ => ""
      }, request),
      ex)

    InternalServerError(Option(Play._currentApp).map {
      case app if app.mode == Play.Mode.Dev => views.html.defaultpages.devError.f
      case app => views.html.defaultpages.error.f
    }.getOrElse(views.html.defaultpages.devError.f) {
      ex match {
        case e: PlayException => e
        case e => UnexpectedException(unexpected = Some(e))
      }
    })
  }

  /**
   * Called when no action was found to serve a request.
   *
   * The default is to send the framework default 404 page.
   *
   * @param request The HTTP request header.
   * @return The result to send to the client.
   */
  def onActionNotFound(request: RequestHeader): Result = {
    NotFound(Option(Play._currentApp).map {
      case app if app.mode == Play.Mode.Dev => views.html.defaultpages.devNotFound.f
      case app => views.html.defaultpages.notFound.f
    }.getOrElse(views.html.defaultpages.devNotFound.f)(request, Option(Play._currentApp).flatMap(_.routes)))
  }

}

/**
 * The default global settings if not defined in the application.
 */
object DefaultGlobal extends GlobalSettings

/**
 * Adapter that holds the java GlobalSettings and acts as a scala GlobalSettings for the framework.
 */
class JavaGlobalSettingsAdapter(javaGlobalSettings: play.GlobalSettings) extends GlobalSettings {
  require(javaGlobalSettings != null, "javaGlobalSettings cannot be null")

  override def beforeStart(app: Application) {
    javaGlobalSettings.beforeStart(new play.Application(app))
  }

  override def onStart(app: Application) {
    javaGlobalSettings.onStart(new play.Application(app))
  }

  override def onStop(app: Application) {
    javaGlobalSettings.onStop(new play.Application(app))
  }

  override def onRouteRequest(request: RequestHeader): Option[Action[_]] = {
    super.onRouteRequest(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Result = {
    Option(javaGlobalSettings.onError(ex)).map(_.getWrappedResult).getOrElse(super.onError(request, ex))
  }

  override def onActionNotFound(request: RequestHeader): Result = {
    Option(javaGlobalSettings.onActionNotFound(request.path)).map(_.getWrappedResult).getOrElse(super.onActionNotFound(request))
  }
}