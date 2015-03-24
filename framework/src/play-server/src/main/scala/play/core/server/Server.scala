/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import com.typesafe.config.ConfigFactory
import play.api.http.{ Port, DefaultHttpErrorHandler }
import play.api.routing.Router

import scala.language.postfixOps

import play.api._
import play.api.mvc._
import play.core.{ TestApplication, DefaultWebCommands, ApplicationProvider }

import scala.util.{ Try, Success, Failure }
import scala.concurrent.Future

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * provides generic server behaviour for Play applications
 */
trait Server extends ServerWithStop {

  def mode: Mode.Mode

  def getHandlerFor(request: RequestHeader): Either[Future[Result], (RequestHeader, Handler, Application)] = {

    import scala.util.control.Exception

    def sendHandler: Try[(RequestHeader, Handler, Application)] = {
      try {
        applicationProvider.get.map { application =>
          application.requestHandler.handlerForRequest(request) match {
            case (requestHeader, handler) => (requestHeader, handler, application)
          }
        }
      } catch {
        case e: ThreadDeath => throw e
        case e: VirtualMachineError => throw e
        case e: Throwable => Failure(e)
      }
    }

    def logExceptionAndGetResult(e: Throwable) = {
      DefaultHttpErrorHandler.onServerError(request, e)
    }

    Exception
      .allCatch[Option[Future[Result]]]
      .either(applicationProvider.handleWebCommand(request).map(Future.successful))
      .left.map(logExceptionAndGetResult)
      .right.flatMap(maybeResult => maybeResult.toLeft(())).right.flatMap { _ =>
        sendHandler match {
          case Failure(e) => Left(logExceptionAndGetResult(e))
          case Success(v) => Right(v)
        }
      }

  }

  def applicationProvider: ApplicationProvider

  def stop() {
    Logger.shutdown()
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
    val server = provider.createServer(config, new TestApplication(application))
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
  def forRouter(router: Router, mode: Mode.Mode, port: Int): Server = {
    val r = router
    val application = new BuiltInComponentsFromContext(ApplicationLoader.Context(
      Environment.simple(mode = mode),
      None, new DefaultWebCommands(), Configuration(ConfigFactory.load())
    )) {
      def router = r
    }.application
    Play.start(application)
    implicitly[ServerProvider].createServer(ServerConfig(mode = mode, port = Some(port)), new TestApplication(application))
  }
}