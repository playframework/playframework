package play.core.j

import play.api._
import play.api.mvc._

/** Adapter that holds the Java `GlobalSettings` and acts as a Scala `GlobalSettings` for the framework. */
class JavaGlobalSettingsAdapter(val underlying: play.GlobalSettings) extends GlobalSettings {
  require(underlying != null, "underlying cannot be null")

  override def beforeStart(app: Application) {
    underlying.beforeStart(new play.Application(app))
  }

  override def onStart(app: Application) {
    underlying.onStart(new play.Application(app))
  }

  override def onStop(app: Application) {
    underlying.onStop(new play.Application(app))
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Result = {
    Option(underlying.onError(ex)).map(_.getWrappedResult).getOrElse(super.onError(request, ex))
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    Option(underlying.onHandlerNotFound(request.path)).map(_.getWrappedResult).getOrElse(super.onHandlerNotFound(request))
  }

  override def onBadRequest(request: RequestHeader, error: String): Result = {
    Option(underlying.onBadRequest(request.path, error)).map(_.getWrappedResult).getOrElse(super.onBadRequest(request, error))
  }

}