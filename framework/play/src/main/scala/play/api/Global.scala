package play.api

import play.api.mvc._

trait GlobalSettings {

  import Results._

  def beforeStart(app: Application) {
  }

  def onStart(app: Application) {
  }

  def onStop(app: Application) {
  }

  def onRouteRequest(request: RequestHeader): Option[Action[_]] = Play._currentApp.routes.flatMap { router =>
    router.actionFor(request)
  }

  def onError(request: RequestHeader, ex: Throwable): Result = {
    Logger.error(
      """
      |
      |! Internal server error, for request [%s] ->
      |""".stripMargin.format(request), ex)
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

  def onActionNotFound(request: RequestHeader): Result = {
    NotFound(Option(Play._currentApp).map {
      case app if app.mode == Play.Mode.Dev => views.html.defaultpages.devNotFound.f
      case app => views.html.defaultpages.notFound.f
    }.getOrElse(views.html.defaultpages.devNotFound.f)(request, Option(Play._currentApp).flatMap(_.routes)))
  }

}

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