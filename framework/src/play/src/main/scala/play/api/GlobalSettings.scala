package play.api

import play.api.mvc._
import java.io.File
import scala.util.control.NonFatal
import scala.concurrent.Future

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
   * Additional configuration provided by the application.  This is invoked by the default implementation of
   * onConfigLoad, so if you override that, this won't be invoked.
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
    val (routedRequest, handler) = onRouteRequest(request) map {
      case handler: RequestTaggingHandler => (handler.tagRequest(request), handler)
      case otherHandler => (request, otherHandler)
    } getOrElse {
      (request, Action.async(BodyParsers.parse.empty)(_ => this.onHandlerNotFound(request)))
    }

    (routedRequest, doFilter(rh => handler)(routedRequest))
  }

  /**
   * Filters.
   */
  def doFilter(next: RequestHeader => Handler): (RequestHeader => Handler) = {
    (request: RequestHeader) =>
      {
        next(request) match {
          case action: EssentialAction => doFilter(action)
          case handler => handler
        }
      }
  }

  /**
   * Filters for EssentialAction.
   */
  def doFilter(next: EssentialAction): EssentialAction = next

  /**
   * Called when an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * @param request the HTTP request header (the body has not been parsed yet)
   * @return an action to handle this request - if no action is returned, a 404 not found result will be sent to client
   * @see onHandlerNotFound
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
  def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
    try {
      Future.successful(InternalServerError(Play.maybeApplication.map {
        case app if app.mode != Mode.Prod => views.html.defaultpages.devError.f
        case app => views.html.defaultpages.error.f
      }.getOrElse(views.html.defaultpages.devError.f) {
        ex match {
          case e: UsefulException => e
          case NonFatal(e) => UnexpectedException(unexpected = Some(e))
        }
      }))
    } catch {
      case e: Throwable => {
        Logger.error("Error while rendering default error page", e)
        Future.successful(InternalServerError)
      }
    }
  }

  /**
   * Called when no action was found to serve a request.
   *
   * The default is to send the framework default 404 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Future.successful(NotFound(Play.maybeApplication.map {
      case app if app.mode != Mode.Prod => views.html.defaultpages.devNotFound.f
      case app => views.html.defaultpages.notFound.f
    }.getOrElse(views.html.defaultpages.devNotFound.f)(request, Play.maybeApplication.flatMap(_.routes))))
  }

  /**
   * Called when an action has been found, but the request parsing has failed.
   *
   * The default is to send the framework default 400 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onBadRequest(request: RequestHeader, error: String): Future[SimpleResult] = {
    Future.successful(BadRequest(views.html.defaultpages.badRequest(request, error)))
  }

  def onRequestCompletion(request: RequestHeader) {
  }

  /**
   * Manages controllers instantiation.
   *
   * @param controllerClass the controller class to instantiate.
   * @return the appropriate instance for the given controller class.
   */
  def getControllerInstance[A](controllerClass: Class[A]): A = {
    controllerClass.newInstance();
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
