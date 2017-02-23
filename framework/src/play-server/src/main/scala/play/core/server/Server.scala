/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import com.typesafe.config.ConfigFactory
import play.api.http.{ Port, DefaultHttpErrorHandler }
import play.api.routing.Router

import scala.language.postfixOps

import play.api._
import play.api.mvc._
import play.core.{ DefaultWebCommands, ApplicationProvider }

import scala.util.{ Success, Failure }
import scala.concurrent.Future

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * Provides generic server behaviour for Play applications.
 */
trait Server extends ServerWithStop {

  def mode: Mode.Mode

  /**
   * Try to get the handler for a request and return it as a `Right`. If we
   * can't get the handler for some reason then return a result immediately
   * as a `Left`. Reasons to return a `Left` value:
   *
   * - If there's a "web command" installed that intercepts the request.
   * - If we fail to get the `Application` from the `applicationProvider`,
   *   i.e. if there's an error loading the application.
   * - If an exception is thrown.
   */
  def getHandlerFor(request: RequestHeader): Either[Future[Result], (RequestHeader, Handler, Application)] = {

    // Common code for handling an exception and returning an error result
    def logExceptionAndGetResult(e: Throwable): Left[Future[Result], Nothing] = {
      Left(DefaultHttpErrorHandler.onServerError(request, e))
    }

    try {
      applicationProvider.handleWebCommand(request) match {
        case Some(result) =>
          Left(Future.successful(result))
        case None =>
          applicationProvider.get match {
            case Success(application) =>
              application.requestHandler.handlerForRequest(request) match {
                case (requestHeader, handler) => Right((requestHeader, handler, application))
              }
            case Failure(e) => logExceptionAndGetResult(e)
          }
      }
    } catch {
      case e: ThreadDeath => throw e
      case e: VirtualMachineError => throw e
      case e: Throwable =>
        logExceptionAndGetResult(e)
    }
  }

  def applicationProvider: ApplicationProvider

  def stop() {
    applicationProvider.current.foreach { app =>
      LoggerConfigurator(app.classloader).foreach(_.shutdown())
    }
  }

  /**
   * Returns the HTTP port of the server.
   *
   * This is useful when the port number has been automatically selected (by setting a port number of 0).
   *
   * @return The HTTP port the server is bound to, if the HTTP connector is enabled.
   */
  def httpPort: Option[Int]

  /**
   * Returns the HTTPS port of the server.
   *
   * This is useful when the port number has been automatically selected (by setting a port number of 0).
   *
   * @return The HTTPS port the server is bound to, if the HTTPS connector is enabled.
   */
  def httpsPort: Option[Int]

}

/**
 * Utilities for creating a server that runs around a block of code.
 */
object Server {

  /**
   * Run a block of code with a server for the given application.
   *
   * The passed in block takes the port that the application is running on. By default, this will be a random ephemeral
   * port. This can be changed by passing in an explicit port with the config parameter.
   *
   * @param application The application for the server to server.
   * @param config The configuration for the server. Defaults to test config with the http port bound to a random
   *               ephemeral port.
   * @param block The block of code to run.
   * @param provider The server provider.
   * @return The result of the block of code.
   */
  def withApplication[T](application: Application, config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test))(block: Port => T)(implicit provider: ServerProvider): T = {
    Play.start(application)
    val server = provider.createServer(config, application)
    try {
      block(new Port((server.httpPort orElse server.httpsPort).get))
    } finally {
      server.stop()
    }
  }

  /**
   * Run a block of code with a server for the given routes.
   *
   * The passed in block takes the port that the application is running on. By default, this will be a random ephemeral
   * port. This can be changed by passing in an explicit port with the config parameter.
   *
   * @param routes The routes for the server to server.
   * @param config The configuration for the server. Defaults to test config with the http port bound to a random
   *               ephemeral port.
   * @param block The block of code to run.
   * @param provider The server provider.
   * @return The result of the block of code.
   */
  def withRouter[T](config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test))(routes: PartialFunction[RequestHeader, Handler])(block: Port => T)(implicit provider: ServerProvider): T = {
    val application = new BuiltInComponentsFromContext(ApplicationLoader.Context(
      Environment.simple(path = config.rootDir, mode = config.mode),
      None, new DefaultWebCommands(), Configuration(ConfigFactory.load())
    )) {
      def router = Router.from(routes)
    }.application
    withApplication(application, config)(block)
  }

}

private[play] object JavaServerHelper {
  def forRouter(router: Router, mode: Mode.Mode, httpPort: Option[Integer], sslPort: Option[Integer]): Server = {
    val r = router
    val application = new BuiltInComponentsFromContext(ApplicationLoader.Context(
      Environment.simple(mode = mode),
      None, new DefaultWebCommands(), Configuration(ConfigFactory.load())
    )) {
      def router = r
    }.application
    Play.start(application)
    val serverConfig = ServerConfig(mode = mode, port = httpPort.map(_.intValue), sslPort = sslPort.map(_.intValue))
    implicitly[ServerProvider].createServer(serverConfig, application)
  }
}
