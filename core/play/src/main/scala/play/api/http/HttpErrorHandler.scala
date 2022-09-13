/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.util.concurrent.CompletionStage

import javax.inject._
import play.api._
import play.api.http.Status._
import play.api.inject.Binding
import play.api.libs.json._
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.mvc.Http
import play.utils.PlayIO
import play.utils.Reflect

import scala.annotation.tailrec
import scala.jdk.FutureConverters._
import scala.concurrent._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

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
) extends PreferredMediaTypeHttpErrorHandler("text/html" -> htmlHandler, "application/json" -> jsonHandler)

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
  private val supportedTypes: Seq[String]                  = handlers.map(_._1)
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
    Reflect.bindingsFromConfiguration[
      HttpErrorHandler,
      DefaultHttpErrorHandler
    ](environment, configuration, "play.http.errorHandler", "ErrorHandler")
  }

  /**
   * Request attributes used by the error handler.
   */
  object Attrs {
    val HttpErrorInfo: TypedKey[HttpErrorInfo] = TypedKey("HttpErrorInfo")
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
    router: => Option[Router] = None
) extends HttpErrorHandler {
  private val logger = Logger(getClass)

  /**
   * @param environment The environment
   * @param router An optional router.
   *               If provided, in dev mode, will be used to display more debug information when a handler can't be found.
   *               This is a lazy parameter, to avoid circular dependency issues, since the router may well depend on
   *               this.
   */
  def this(
      environment: Environment,
      configuration: Configuration,
      router: => Option[Router]
  ) =
    this(
      HttpErrorConfig(environment.mode != Mode.Prod, configuration.getOptional[String]("play.editor")),
      router
    )

  @Inject
  def this(
      environment: Environment,
      configuration: Configuration,
      router: Provider[Router]
  ) =
    this(environment, configuration, Some(router.get))

  // Hyperlink string to wrap around Play error messages.
  private var playEditor: Option[String] = config.playEditor

  /**
   * Sets the play editor to the given string after initialization.  Used for
   * tests, or cases where the existing configuration isn't sufficient.
   *
   * @param editor the play editor string.
   */
  def setPlayEditor(editor: String): Unit = playEditor = Option(editor)

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    statusCode match {
      case BAD_REQUEST                                          => onBadRequest(request, message)
      case FORBIDDEN                                            => onForbidden(request, message)
      case NOT_FOUND                                            => onNotFound(request, message)
      case clientError if statusCode >= 400 && statusCode < 500 => onOtherClientError(request, statusCode, message)
      case nonClientError =>
        throw new IllegalArgumentException(
          s"onClientError invoked with non client error status code $statusCode: $message"
        )
    }

  /**
   * Invoked when a client makes a bad request.
   *
   * @param request The request that was bad.
   * @param message The error message.
   */
  protected def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    Future.successful(BadRequest(s"bad request: $message"))

  /**
   * Invoked when a client makes a request that was forbidden.
   *
   * @param request The forbidden request.
   * @param message The error message.
   */
  protected def onForbidden(request: RequestHeader, message: String): Future[Result] =
    Future.successful(Forbidden(s"forbidden: $message"))

  /**
   * Invoked when a handler or resource is not found.
   *
   * @param request The request that no handler was found to handle.
   * @param message A message.
   */
  protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful {
      NotFound(s"not found: $message")
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
      Results.Status(statusCode)(s"client error: $message")
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
      val usefulException =
        HttpErrorHandlerExceptions.throwableToUsefulException(!config.showDevErrors, exception)

      logServerError(request, usefulException)

      onProdServerError(request, usefulException)
    } catch {
      case NonFatal(e) =>
        logger.error("Error while handling error", e)
        Future.successful(InternalServerError(fatalErrorMessage(request, e)))
    }
  }

  /**
   * Invoked when handling a server error with this error handler failed.
   *
   * <p>As a last resort this method allows you to return a (simple) error message that will be send
   * along with a "500 Internal Server Error" response. It's highly recommended to just return a
   * simple string, without doing any fancy processing inside the method (like accessing files,...)
   * that could throw exceptions. This is your last chance to send a meaningful error message when
   * everything else failed.
   *
   * @param request   The request that triggered the server error.
   * @param exception The server error.
   * @return An error message which will be send as last resort in case handling a server error with
   *         this error handler failed.
   */
  protected def fatalErrorMessage(request: RequestHeader, exception: Throwable): String = ""

  /**
   * Responsible for logging server errors.
   *
   * This can be overridden to add additional logging information, eg. the id of the authenticated user.
   *
   * @param request The request that triggered the server error.
   * @param usefulException The server error.
   */
  protected def logServerError(request: RequestHeader, usefulException: UsefulException): Unit = {
    logger.error(
      """
        |
        |! @%s - Internal server error, for (%s) [%s] ->
        | """.stripMargin.format(usefulException.id, request.method, request.uri),
      usefulException
    )
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
      InternalServerError("server error")
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
  @tailrec def throwableToUsefulException(
      isProd: Boolean,
      throwable: Throwable
  ): UsefulException = throwable match {
    case useful: UsefulException => useful
    case e: ExecutionException   => throwableToUsefulException(isProd, e.getCause)
    case prodException if isProd => UnexpectedException(unexpected = Some(prodException))
    case other =>
      val desc = s"[${other.getClass.getSimpleName}: ${other.getMessage}]"
      new PlayException.ExceptionSource("Execution exception", desc, other) {
        def line       = null
        def position   = null
        def input      = null
        def sourceName = null
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
class JsonHttpErrorHandler(environment: Environment) extends HttpErrorHandler {
  private val logger = Logger(getClass)

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
    if (play.api.http.Status.isClientError(statusCode)) {
      Future.successful(Results.Status(statusCode)(error(Json.obj("requestId" -> request.id, "message" -> message))))
    } else {
      throw new IllegalArgumentException(
        s"onClientError invoked with non client error status code $statusCode: $message"
      )
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
        isProd,
        exception
      )
      logServerError(request, usefulException)
      Future.successful(
        InternalServerError(
          prodServerError(request, usefulException)
        )
      )
    } catch {
      case NonFatal(e) =>
        logger.error("Error while handling error", e)
        Future.successful(InternalServerError(fatalErrorJson(request, e)))
    }

  /**
   * Invoked when handling a server error with this error handler failed.
   *
   * <p>As a last resort this method allows you to return a (simple) error message that will be send
   * along with a "500 Internal Server Error" response. It's highly recommended to just return a
   * simple JsonNode, without doing any fancy processing inside the method (like accessing files,...)
   * that could throw exceptions. This is your last chance to send a meaningful error message when
   * everything else failed.
   *
   * @param request   The request that triggered the server error.
   * @param exception The server error.
   * @return An error JSON which will be send as last resort in case handling a server error with
   *         this error handler failed.
   */
  protected def fatalErrorJson(request: RequestHeader, exception: Throwable): JsValue = Json.obj()

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
    logger.error(
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
object DefaultHttpErrorHandler
    extends DefaultHttpErrorHandler(HttpErrorConfig(showDevErrors = true, playEditor = None), None) {
  private lazy val setEditor: Unit = {
    val conf = Configuration.load(Environment.simple())
    conf.getOptional[String]("play.editor").foreach(setPlayEditor)
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
