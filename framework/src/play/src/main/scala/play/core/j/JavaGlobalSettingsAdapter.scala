package play.core.j

import play.api._
import play.api.mvc._
import java.io.File
import scala.concurrent.Future
import play.api.libs.iteratee._

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
    val r = JavaHelpers.createJavaRequest(request)
    Option(underlying.onRouteRequest(r)).map(Some(_)).getOrElse(super.onRouteRequest(request))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
    JavaHelpers.invokeWithContext(request, req => Option(underlying.onError(req, ex)))
      .getOrElse(super.onError(request, ex))
  }

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    JavaHelpers.invokeWithContext(request, req => Option(underlying.onHandlerNotFound(req)))
      .getOrElse(super.onHandlerNotFound(request))
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[SimpleResult] = {
    JavaHelpers.invokeWithContext(request, req => Option(underlying.onBadRequest(req, error)))
      .getOrElse(super.onBadRequest(request, error))
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    Option(underlying.getControllerInstance(controllerClass))
      .getOrElse(super.getControllerInstance(controllerClass))
  }

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode) = {
    Option(underlying.onLoadConfig(new play.Configuration(config), path, classloader))
      .map(_.getWrappedConfiguration).getOrElse(super.onLoadConfig(config, path, classloader, mode))
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    try {
      Filters(super.doFilter(a), underlying.filters.map(_.newInstance: play.api.mvc.EssentialFilter): _*)
    } catch {
      case e: Throwable => {
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        EssentialAction(req => Iteratee.flatten(onError(req, e).map(result => Done(result, Input.Empty))))
      }
    }
  }

}
