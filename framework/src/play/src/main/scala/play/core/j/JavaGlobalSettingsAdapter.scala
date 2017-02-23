/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import play.api._
import play.api.libs.streams.Accumulator
import play.api.mvc._
import scala.concurrent.Future
import scala.util.control.NonFatal

/** Adapter that holds the Java `GlobalSettings` and acts as a Scala `GlobalSettings` for the framework. */
class JavaGlobalSettingsAdapter(val underlying: play.GlobalSettings) extends GlobalSettings.Deprecated {
  require(underlying != null, "underlying cannot be null")

  override def beforeStart(app: Application) {
    underlying.beforeStart(app.injector.instanceOf[play.Application])
  }

  override def onStart(app: Application) {
    underlying.onStart(app.injector.instanceOf[play.Application])
  }

  override def onStop(app: Application) {
    underlying.onStop(app.injector.instanceOf[play.Application])
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val r = new play.mvc.Http.RequestImpl(request)
    Option(underlying.onRouteRequest(r)).map(Some(_)).getOrElse(super.onRouteRequest(request))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    JavaHelpers.invokeWithContextOpt(request, req => underlying.onError(req, ex))
      .getOrElse(super.onError(request, ex))
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    JavaHelpers.invokeWithContextOpt(request, req => underlying.onHandlerNotFound(req))
      .getOrElse(super.onHandlerNotFound(request))
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    JavaHelpers.invokeWithContextOpt(request, req => underlying.onBadRequest(req, error))
      .getOrElse(super.onBadRequest(request, error))
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    try {
      Filters(super.doFilter(a), underlying.filters.map(_.newInstance: play.api.mvc.EssentialFilter): _*)
    } catch {
      case NonFatal(e) => {
        EssentialAction(req => Accumulator.done(onError(req, e)))
      }
    }
  }

}
