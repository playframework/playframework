package play.core.j

import play.api._
import play.api.mvc._

/** Adapter that holds the Java `GlobalSettings` and acts as a Scala `GlobalSettings` for the framework. */
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

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Result = {
    Option(javaGlobalSettings.onError(ex)).map(_.getWrappedResult).getOrElse(super.onError(request, ex))
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    Option(javaGlobalSettings.onHandlerNotFound(request.path)).map(_.getWrappedResult).getOrElse(super.onHandlerNotFound(request))
  }

  override def onBadRequest(request: RequestHeader, error: String): Result = {
    Option(javaGlobalSettings.onBadRequest(request.path, error)).map(_.getWrappedResult).getOrElse(super.onBadRequest(request, error))
  }

}