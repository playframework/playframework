/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.util.concurrent.CompletionStage

import javax.inject._
import play.api._
import play.api.http.Status._
import play.api.inject.Binding
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.core.SourceMapper
import play.core.j.JavaHttpErrorHandlerAdapter
import play.libs.exception.ExceptionUtils
import play.mvc.Http
import play.utils.{ PlayIO, Reflect }

import scala.compat.java8.FutureConverters
import scala.concurrent._
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Component for handling HTTP errors in Play.
 *
 * @since 2.4.0
 */
trait HttpErrorHandler {

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result]

  /**
   * Invoked when a server error occurs.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   */
  def onServerError(request: RequestHeader, exception: Throwable): Future[Result]
}

/**
 * An [[HttpErrorHandler]] that uses either HTML or JSON in the response depending on the client's preference.
 */
class HtmlOrJsonHttpErrorHandler @Inject() (
    htmlHandler: DefaultHttpErrorHandler,
    jsonHandler: JsonHttpErrorHandler
) extends PreferredMediaTypeHttpErrorHandler(
  "text/html" -> htmlHandler,
  "application/json" -> jsonHandler
)

/**
 * An [[HttpErrorHandler]] that delegates to one of several [[HttpErrorHandler]]s based on media type preferences.
 *
 * For example, to create an error handler that handles JSON and HTML, with JSON preferred by the app as default:
 * {{{
 *   override lazy val httpErrorHandler = PreferredMediaTypeHttpErrorHandler(
 *     "application/json" -> new JsonHttpErrorHandler()
 *     "text/html" -> new HtmlHttpErrorHandler(),
 *   )
 * }}}
 *
 * If the client's preferred media range matches multiple media types in the list, then the first match is chosen.
 */
class PreferredMediaTypeHttpErrorHandler(val handlers: (String, HttpErrorHandler)*) extends HttpErrorHandler {

  private val supportedTypes: Seq[String] = handlers.map(_._1)
  private val typeToHandler: Map[String, HttpErrorHandler] = handlers.toMap

  protected val defaultHandler: HttpErrorHandler =
    handlers.headOption.getOrElse(throw new IllegalArgumentException("handlers must not be empty"))._2

  protected def preferredHandler(request: RequestHeader): HttpErrorHandler = {
    MediaRange.preferred(request.acceptedTypes, supportedTypes).fold(defaultHandler)(typeToHandler)
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    preferredHandler(request).onClientError(request, statusCode, message)
  }
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    preferredHandler(request).onServerError(request, exception)
  }
}

object PreferredMediaTypeHttpErrorHandler {
  def apply(handlers: (String, HttpErrorHandler)*) = new PreferredMediaTypeHttpErrorHandler(handlers: _*)
}

object HttpErrorHandler {

  /**
   * Get the bindings for the error handler from the configuration
   */
  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[HttpErrorHandler, play.http.HttpErrorHandler, JavaHttpErrorHandlerAdapter, JavaHttpErrorHandlerDelegate, DefaultHttpErrorHandler](environment, configuration,
      "play.http.errorHandler", "ErrorHandler")
  }
}

case class HttpErrorConfig(showDevErrors: Boolean = false, playEditor: Option[String] = None)

/**
 * The default HTTP error handler.
 *
 * This class is intended to be extended, allowing users to reuse some of the functionality provided here.
 *
 * @param router An optional router.
 *               If provided, in dev mode, will be used to display more debug information when a handler can't be found.
 *               This is a lazy parameter, to avoid circular dependency issues, since the router may well depend on
 *               this.
 */
@Singleton
class DefaultHttpErrorHandler(
    config: HttpErrorConfig = HttpErrorConfig(),
    sourceMapper: Option[SourceMapper] = None,
    router: => Option[Router] = None) extends HttpErrorHandler {

  /**
   * @param environment The environment
   * @param router An optional router.
   *               If provided, in dev mode, will be used to display more debug information when a handler can't be found.
   *               This is a lazy parameter, to avoid circular dependency issues, since the router may well depend on
   *               this.
   */
  def this(environment: Environment, configuration: Configuration, sourceMapper: Option[SourceMapper], router: => Option[Router]) =
    this(HttpErrorConfig(environment.mode != Mode.Prod, configuration.getOptional[String]("play.editor")), sourceMapper, router)

  @Inject
  def this(environment: Environment, configuration: Configuration, sourceMapper: OptionalSourceMapper, router: Provider[Router]) =
    this(environment, configuration, sourceMapper.sourceMapper, Some(router.get))

  // Hyperlink string to wrap around Play error messages.
  private var playEditor: Option[String] = config.playEditor

  /**
   * Sets the play editor to the given string after initialization.  Used for
   * tests, or cases where the existing configuration isn't sufficient.
   *
   * @param editor the play editor string.
   */
  def setPlayEditor(editor: String): Unit = {
    playEditor = Option(editor)
  }

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = statusCode match {
    case BAD_REQUEST => onBadRequest(request, message)
    case FORBIDDEN => onForbidden(request, message)
    case NOT_FOUND => onNotFound(request, message)
    case clientError if statusCode >= 400 && statusCode < 500 => onOtherClientError(request, statusCode, message)
    case nonClientError =>
      throw new IllegalArgumentException(s"onClientError invoked with non client error status code $statusCode: $message")
  }

  /**
   * Invoked when a client makes a bad request.
   *
   * @param request The request that was bad.
   * @param message The error message.
   */
  protected def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    Future.successful {
      implicit val ir: RequestHeader = request
      BadRequest(views.html.defaultpages.badRequest(request.method, request.uri, message))
    }

  /**
   * Invoked when a client makes a request that was forbidden.
   *
   * @param request The forbidden request.
   * @param message The error message.
   */
  protected def onForbidden(request: RequestHeader, message: String): Future[Result] =
    Future.successful {
      implicit val ir: RequestHeader = request
      Forbidden(views.html.defaultpages.unauthorized())
    }

  /**
   * Invoked when a handler or resource is not found.
   *
   * @param request The request that no handler was found to handle.
   * @param message A message.
   */
  protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful {
      implicit val ir: RequestHeader = request
      if (config.showDevErrors) {
        NotFound(views.html.defaultpages.devNotFound(request.method, request.uri, router))
      } else {
        NotFound(views.html.defaultpages.notFound(request.method, request.uri))
      }
    }
  }

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled by any of
   * the other methods in this class already.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful {
      implicit val ir: RequestHeader = request
      Results.Status(statusCode)(views.html.defaultpages.badRequest(request.method, request.uri, message))
    }
  }

  /**
   * Invoked when a server error occurs.
   *
   * By default, the implementation of this method delegates to [[onProdServerError]] when in prod mode, and
   * [[onDevServerError]] in dev mode.  It is recommended, if you want Play's debug info on the error page in dev
   * mode, that you override [[onProdServerError]] instead of this method.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   */
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    try {
      val usefulException = HttpErrorHandlerExceptions.throwableToUsefulException(
        sourceMapper,
        !config.showDevErrors, exception)

      logServerError(request, usefulException)

      if (config.showDevErrors) onDevServerError(request, usefulException)
      else onProdServerError(request, usefulException)

    } catch {
      case NonFatal(e) =>
        Logger.error("Error while handling error", e)
        Future.successful(InternalServerError)
    }
  }

  /**
   * Responsible for logging server errors.
   *
   * This can be overridden to add additional logging information, eg. the id of the authenticated user.
   *
   * @param request The request that triggered the server error.
   * @param usefulException The server error.
   */
  protected def logServerError(request: RequestHeader, usefulException: UsefulException): Unit = {
    Logger.error(
      """
                    |
                    |! @%s - Internal server error, for (%s) [%s] ->
                    | """.stripMargin.format(usefulException.id, request.method, request.uri),
      usefulException
    )
  }

  /**
   * Invoked in dev mode when a server error occurs.
   *
   * @param request The request that triggered the error.
   * @param exception The exception.
   */
  protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful {
      implicit val ir: RequestHeader = request
      InternalServerError(views.html.defaultpages.devError(playEditor, exception))
    }
  }

  /**
   * Invoked in prod mode when a server error occurs.
   *
   * Override this rather than [[onServerError]] if you don't want to change Play's debug output when logging errors
   * in dev mode.
   *
   * @param request The request that triggered the error.
   * @param exception The exception.
   */
  protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful {
      implicit val ir: RequestHeader = request
      InternalServerError(views.html.defaultpages.error(exception))
    }

}

/**
 * Extracted so the Java default error handler can reuse this functionality
 */
object HttpErrorHandlerExceptions {

  /**
   * Convert the given exception to an exception that Play can report more information about.
   *
   * This will generate an id for the exception, and in dev mode, will load the source code for the code that threw the
   * exception, making it possible to report on the location that the exception was thrown from.
   */
  def throwableToUsefulException(sourceMapper: Option[SourceMapper], isProd: Boolean, throwable: Throwable): UsefulException = throwable match {
    case useful: UsefulException => useful
    case e: ExecutionException => throwableToUsefulException(sourceMapper, isProd, e.getCause)
    case prodException if isProd => UnexpectedException(unexpected = Some(prodException))
    case other =>
      val source = sourceMapper.flatMap(_.sourceFor(other))

      new PlayException.ExceptionSource(
        "Execution exception",
        "[%s: %s]".format(other.getClass.getSimpleName, other.getMessage),
        other) {
        def line = source.flatMap(_._2).map(_.asInstanceOf[java.lang.Integer]).orNull
        def position = null
        def input = source.map(_._1).map(f => PlayIO.readFileAsString(f.toPath)).orNull
        def sourceName = source.map(_._1.getAbsolutePath).orNull
      }
  }
}

/**
 * An alternative default HTTP error handler which will render errors as JSON messages instead of HTML pages.
 *
 * In Dev mode, exceptions thrown by the server code will be rendered in JSON messages.
 * In Prod mode, they will not be rendered.
 *
 * You could override how exceptions are rendered in Dev mode by extending this class and overriding
 * the [[formatDevServerErrorException]] method.
 */
class JsonHttpErrorHandler(
    environment: Environment,
    sourceMapper: Option[SourceMapper] = None) extends HttpErrorHandler {

  @Inject
  def this(environment: Environment, optionalSourceMapper: OptionalSourceMapper) = {
    this(environment, optionalSourceMapper.sourceMapper)
  }

  @inline
  private final def error(content: JsObject): JsObject = Json.obj("error" -> content)

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (Status.isClientError(statusCode)) {
      Future.successful(Results.Status(statusCode)(error(Json.obj("requestId" -> request.id, "message" -> message))))
    } else {
      throw new IllegalArgumentException(s"onClientError invoked with non client error status code $statusCode: $message")
    }
  }

  /**
   * Invoked when a server error occurs.
   *
   * @param request The request that triggered the server error.
   * @param exception The server error.
   */
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    try {
      val isProd = environment.mode == Mode.Prod

      val usefulException = HttpErrorHandlerExceptions.throwableToUsefulException(
        sourceMapper,
        isProd,
        exception
      )
      logServerError(request, usefulException)
      Future.successful(
        InternalServerError(
          if (isProd) prodServerError(request, usefulException)
          else devServerError(request, usefulException)
        )
      )
    } catch {
      case NonFatal(e) =>
        Logger.error("Error while handling error", e)
        Future.successful(InternalServerError)
    }

  protected def devServerError(request: RequestHeader, exception: UsefulException): JsValue = {
    error(Json.obj(
      "id" -> exception.id,
      "requestId" -> request.id,
      "exception" -> Json.obj(
        "title" -> exception.title,
        "description" -> exception.description,
        "stacktrace" -> formatDevServerErrorException(exception.cause)
      )
    ))
  }

  /**
   * Format a [[Throwable]] as a JSON value.
   *
   * Override this method if you want to change how exceptions are rendered in Dev mode.
   *
   * @param exception
   * @return
   */
  protected def formatDevServerErrorException(exception: Throwable): JsValue =
    JsArray(ExceptionUtils.getStackFrames(exception).map(s => JsString(s.trim)))

  protected def prodServerError(request: RequestHeader, exception: UsefulException): JsValue =
    error(Json.obj("id" -> exception.id))

  /**
   * Responsible for logging server errors.
   *
   * This can be overridden to add additional logging information, eg. the id of the authenticated user.
   *
   * @param request The request that triggered the server error.
   * @param usefulException The server error.
   */
  protected def logServerError(request: RequestHeader, usefulException: UsefulException): Unit = {
    Logger.error(
      """
        |
        |! @%s - Internal server error, for (%s) [%s] ->
        | """.stripMargin.format(usefulException.id, request.method, request.uri),
      usefulException
    )
  }

}

/**
 * A default HTTP error handler that can be used when there's no application available.
 *
 * Note: this HttpErrorHandler should ONLY be used in DEV or TEST. The way this displays errors to the user is
 * generally not suitable for a production environment.
 */
object DefaultHttpErrorHandler extends DefaultHttpErrorHandler(
  HttpErrorConfig(showDevErrors = true, playEditor = None), None, None) {

  private lazy val setEditor: Unit = {
    val conf = Configuration.load(Environment.simple())
    conf.getOptional[String]("play.editor") foreach setPlayEditor
  }
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    setEditor
    super.onClientError(request, statusCode, message)
  }
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    setEditor
    super.onServerError(request, exception)
  }
}

/**
 * A lazy HTTP error handler, that looks up the error handler from the current application
 */
@deprecated("Access the global state. Inject a HttpErrorHandler instead", "2.7.0")
object LazyHttpErrorHandler extends HttpErrorHandler {

  private def errorHandler: HttpErrorHandler = Play.privateMaybeApplication match {
    case Success(app) => app.errorHandler
    case Failure(_) => DefaultHttpErrorHandler
  }

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    errorHandler.onClientError(request, statusCode, message)

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    errorHandler.onServerError(request, exception)
}

/**
 * A Java error handler that's provided when a Scala one is configured, so that Java code can still have the error
 * handler injected.
 */
private[play] class JavaHttpErrorHandlerDelegate @Inject() (delegate: HttpErrorHandler) extends play.http.HttpErrorHandler {
  import play.core.Execution.Implicits.trampoline

  def onClientError(request: Http.RequestHeader, statusCode: Int, message: String): CompletionStage[play.mvc.Result] =
    FutureConverters.toJava(delegate.onClientError(request.asScala(), statusCode, message).map(_.asJava))

  def onServerError(request: Http.RequestHeader, exception: Throwable): CompletionStage[play.mvc.Result] =
    FutureConverters.toJava(delegate.onServerError(request.asScala(), exception).map(_.asJava))
}
