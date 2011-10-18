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

  def onError(ex: Throwable): Result = {
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