/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.URI
import java.util.function.{ Function => JFunction }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.annotation.ApiMayChange
import play.{ ApplicationLoader => JApplicationLoader }
import play.{ BuiltInComponents => JBuiltInComponents }
import play.{ BuiltInComponentsFromContext => JBuiltInComponentsFromContext }
import play.api._
import play.api.http.HttpErrorHandler
import play.api.http.Port
import play.api.inject.ApplicationLifecycle
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.mvc.request.RequestTarget
import play.api.routing.Router
import play.api.ApplicationLoader.Context
import play.core._
import play.routing.{ Router => JRouter }

trait WebSocketable {
  def getHeader(header: String): String
  def check: Boolean
}

/**
 * Provides generic server behaviour for Play applications.
 */
trait Server extends ReloadableServer {
  def mode: Mode

  def applicationProvider: ApplicationProvider

  def reload(): Unit = applicationProvider.get

  def stop(): Unit = {
    applicationProvider.get.foreach { app => LoggerConfigurator(app.classloader).foreach(_.shutdown()) }
  }

  /**
   * Get the address of the server.
   *
   * @return The address of the server.
   */
  def mainAddress: java.net.InetSocketAddress

  /**
   * Returns the HTTP port of the server.
   *
   * This is useful when the port number has been automatically selected (by setting a port number of 0).
   *
   * @return The HTTP port the server is bound to, if the HTTP connector is enabled.
   */
  def httpPort: Option[Int] = serverEndpoints.httpEndpoint.map(_.port)

  /**
   * Returns the HTTPS port of the server.
   *
   * This is useful when the port number has been automatically selected (by setting a port number of 0).
   *
   * @return The HTTPS port the server is bound to, if the HTTPS connector is enabled.
   */
  def httpsPort: Option[Int] = serverEndpoints.httpsEndpoint.map(_.port)

  /**
   * Endpoints information for this server.
   */
  @ApiMayChange
  def serverEndpoints: ServerEndpoints
}

/**
 * Utilities for creating a server that runs around a block of code.
 */
object Server {

  private val logger = Logger(getClass)

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
  private[server] def getHandlerFor(
      request: RequestHeader,
      tryApp: Try[Application],
      fallbackErrorHandler: HttpErrorHandler
  ): (RequestHeader, Handler) = {
    @inline def handleErrors(
        errorHandler: HttpErrorHandler,
        req: RequestHeader
    ): PartialFunction[Throwable, (RequestHeader, Handler)] = {
      case e: ThreadDeath         => throw e
      case e: VirtualMachineError => throw e
      case e: Throwable           =>
        val errorResult = errorHandler.onServerError(req, e)
        val errorAction = actionForResult(errorResult)
        (req, errorAction)
    }

    try {
      // Get the Application from the try.
      val application = tryApp.get
      // We managed to get an Application, now make a fresh request using the Application's RequestFactory.
      // The request created by the request factory needs to be at this scope so that it can be
      // used by application error handler. The reason for that is that this request is populated
      // with all attributes necessary to translate it to Java.
      // TODO: `copyRequestHeader` is a misleading name here since it is also populating the request with attributes
      //       such as id, session, flash, etc.
      val enrichedRequest: RequestHeader = application.requestFactory.copyRequestHeader(request)
      try {
        // We hen use the Application's logic to handle that request.
        val (handlerHeader, handler) = application.requestHandler.handlerForRequest(enrichedRequest)
        (handlerHeader, handler)
      } catch {
        handleErrors(application.errorHandler, enrichedRequest)
      }
    } catch {
      handleErrors(fallbackErrorHandler, request)
    }
  }

  /**
   * Create a simple [[Handler]] which sends a [[Result]].
   */
  private[server] def actionForResult(errorResult: Future[Result]): Handler = {
    EssentialAction(_ => Accumulator.done(errorResult))
  }

  /**
   * Create request target information from a request uri String where
   * there was a parsing failure.
   */
  private[server] def createUnparsedRequestTarget(requestUri: String): RequestTarget = new RequestTarget {
    override lazy val uri: URI = new URI(uriString)

    override def uriString: String = requestUri

    override lazy val path: String = {
      // The URI may be invalid, so instead, do a crude heuristic to drop the host and query string from it to get the
      // path, and don't decode.
      // RICH: This looks like a source of potential security bugs to me!
      val withoutHost        = uriString.dropWhile(_ != '/')
      val withoutQueryString = withoutHost.split('?').head
      if (withoutQueryString.isEmpty) "/" else withoutQueryString
    }
    override lazy val queryMap: Map[String, Seq[String]] = {
      // Very rough parse of query string that doesn't decode
      if (requestUri.contains("?")) {
        requestUri
          .split("\\?", 2)(1)
          .split('&')
          .map { keyPair =>
            keyPair.split("=", 2) match {
              case Array(key)        => key -> ""
              case Array(key, value) => key -> value
            }
          }
          .groupBy(_._1)
          .map {
            case (name, values) => name -> values.map(_._2).toSeq
          }
      } else {
        Map.empty
      }
    }
  }

  /**
   * Parses the config setting `infinite` as `Long.MaxValue` otherwise uses Config's built-in
   * parsing of byte values.
   */
  private[server] def getPossiblyInfiniteBytes(
      config: Config,
      path: String,
      deprecatedPath: String = """"""""
  ): Long = {
    Configuration(config).getDeprecated[String](path, deprecatedPath) match {
      case "infinite" => Long.MaxValue
      case _          => config.getBytes(if (config.hasPath(deprecatedPath)) deprecatedPath else path)
    }
  }

  /**
   * Determines the timeout after a server is forcefully stopped.
   * The termination hard-deadline is either what was configured by the user or defaults to `service-requests-done` phase timeout.
   * Also warns about timeout mismatches in case the user configured time out is higher then the `service-requests-done` phase timeout.
   */
  private[server] def determineServerTerminateTimeout(
      terminationTimeout: Option[FiniteDuration],
      terminationDelay: FiniteDuration
  )(implicit actorSystem: ActorSystem): FiniteDuration = {
    val cs                         = CoordinatedShutdown(actorSystem)
    val serviceRequestsDoneTimeout = cs.timeout(CoordinatedShutdown.PhaseServiceRequestsDone)
    val serverTerminateTimeout     = terminationTimeout.getOrElse(serviceRequestsDoneTimeout)
    if (serverTerminateTimeout > serviceRequestsDoneTimeout)
      logger.warn(
        s"""The value for `play.server.terminationTimeout` [$serverTerminateTimeout] is higher than the total `service-requests-done.timeout` duration [$serviceRequestsDoneTimeout].
           |Set `pekko.coordinated-shutdown.phases.service-requests-done.timeout` to an equal (or greater) value to prevent unexpected server termination.""".stripMargin
      )
    else if (terminationDelay.length > 0 && (terminationDelay + serverTerminateTimeout) > serviceRequestsDoneTimeout)
      logger.warn(
        s"""The total of `play.server.waitBeforeTermination` [$terminationDelay]` and `play.server.terminationTimeout` [$serverTerminateTimeout], which is ${terminationDelay + serverTerminateTimeout}, is higher than the total `service-requests-done.timeout` duration [$serviceRequestsDoneTimeout].
           |Set `pekko.coordinated-shutdown.phases.service-requests-done.timeout` to an equal (or greater) value to prevent unexpected server termination.""".stripMargin
      )
    serverTerminateTimeout
  }

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
  def withApplication[T](
      application: Application,
      config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)
  )(block: Port => T)(implicit provider: ServerProvider): T = {
    Play.start(application)
    val server = provider.createServer(config, application)
    try {
      block(new Port(server.httpPort.orElse(server.httpsPort).get))
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
  def withRouter[T](
      config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)
  )(routes: PartialFunction[RequestHeader, Handler])(block: Port => T)(implicit provider: ServerProvider): T = {
    val context = ApplicationLoader.Context(
      environment = Environment.simple(path = config.rootDir, mode = config.mode),
      initialConfiguration = Configuration(ConfigFactory.load()),
      lifecycle = new DefaultApplicationLifecycle,
      devContext = None
    )
    val application = new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      def router = Router.from(routes)
    }.application
    withApplication(application, config)(block)
  }

  /**
   * Run a block of code with a server for the given routes, obtained from the application components
   *
   * The passed in block takes the port that the application is running on. By default, this will be a random ephemeral
   * port. This can be changed by passing in an explicit port with the config parameter.
   *
   * @param routes A function that obtains the routes from the server from the application components.
   * @param config The configuration for the server. Defaults to test config with the http port bound to a random
   *               ephemeral port.
   * @param block The block of code to run.
   * @param provider The server provider.
   * @return The result of the block of code.
   */
  def withRouterFromComponents[T](config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test))(
      routes: BuiltInComponents => PartialFunction[RequestHeader, Handler]
  )(block: Port => T)(implicit provider: ServerProvider): T = {
    val context: Context = ApplicationLoader.Context(
      environment = Environment.simple(path = config.rootDir, mode = config.mode),
      initialConfiguration = Configuration(ConfigFactory.load()),
      lifecycle = new DefaultApplicationLifecycle,
      devContext = None
    )
    val application =
      (new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents { self: BuiltInComponents =>
        def router = Router.from(routes(self))
      }).application
    withApplication(application, config)(block)
  }

  /**
   * Run a block of code with a server for the application containing routes.
   *
   * The passed in block takes the port that the application is running on. By default, this will be a random ephemeral
   * port. This can be changed by passing in an explicit port with the config parameter.
   *
   * An easy way to set up an application with given routes is to use [[play.api.BuiltInComponentsFromContext]] with
   * any extra components needed:
   *
   * {{{
   *   Server.withApplicationFromContext(ServerConfig(mode = Mode.Prod, port = Some(0))) { context =>
   *     new BuiltInComponentsFromContext(context) with AssetsComponents with play.filters.HttpFiltersComponents {
   *      override def router: Router = Router.from {
   *        case req => assets.versioned("/testassets", req.path)
   *      }
   *    }.application
   *  } { withClient(block)(_) }
   * }}}
   *
   * @param appProducer A function that takes an ApplicationLoader.Context and produces [[play.api.Application]]
   * @param config The configuration for the server. Defaults to test config with the http port bound to a random
   *               ephemeral port.
   * @param block The block of code to run.
   * @param provider The server provider.
   * @return The result of the block of code.
   */
  def withApplicationFromContext[T](
      config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)
  )(appProducer: ApplicationLoader.Context => Application)(block: Port => T)(implicit provider: ServerProvider): T = {
    val context: Context = ApplicationLoader.Context(
      environment = Environment.simple(path = config.rootDir, mode = config.mode),
      initialConfiguration = Configuration(ConfigFactory.load()),
      lifecycle = new DefaultApplicationLifecycle,
      devContext = None
    )
    withApplication(appProducer(context), config)(block)
  }

  case object ServerStoppedReason extends CoordinatedShutdown.Reason

  private[server] def routeModifierDefersBodyParsing(global: Boolean, rh: RequestHeader): Boolean = {
    import play.api.routing.Router.RequestImplicits._
    (global || rh.hasRouteModifier("deferBodyParsing")) && !rh.hasRouteModifier("dontDeferBodyParsing")
  }
}

/**
 * Components to create a Server instance.
 */
trait ServerComponents {
  def server: Server

  lazy val serverConfig: ServerConfig = ServerConfig()

  lazy val environment: Environment                   = Environment.simple(mode = serverConfig.mode)
  lazy val configuration: Configuration               = Configuration(ConfigFactory.load())
  lazy val applicationLifecycle: ApplicationLifecycle = new DefaultApplicationLifecycle

  def serverStopHook: () => Future[Unit] = () => Future.successful(())
}

/**
 * Define how to create a Server from a Router.
 */
private[server] trait ServerFromRouter {
  protected def createServerFromRouter(serverConfig: ServerConfig = ServerConfig())(
      routes: ServerComponents with BuiltInComponents => Router
  ): Server

  /**
   * Creates a [[Server]] from the given router.
   *
   * @param config the server configuration
   * @param routes the routes definitions
   * @return an PekkoHttpServer instance
   */
  @deprecated(
    "Use fromRouterWithComponents or use DefaultPekkoHttpServerComponents/DefaultNettyServerComponents",
    "2.7.0"
  )
  def fromRouter(config: ServerConfig = ServerConfig())(routes: PartialFunction[RequestHeader, Handler]): Server = {
    createServerFromRouter(config) { _ => Router.from(routes) }
  }

  /**
   * Creates a [[Server]] from the given router, using [[ServerComponents]].
   *
   * @param config the server configuration
   * @param routes the routes definitions
   * @return an PekkoHttpServer instance
   */
  def fromRouterWithComponents(
      config: ServerConfig = ServerConfig()
  )(routes: BuiltInComponents => PartialFunction[RequestHeader, Handler]): Server = {
    createServerFromRouter(config)(components => Router.from(routes(components)))
  }
}

private[play] object JavaServerHelper {
  def forRouter(router: JRouter, mode: Mode, httpPort: Option[Integer], sslPort: Option[Integer]): Server = {
    forRouter(mode, httpPort, sslPort)(_ => router)
  }

  def forRouter(mode: Mode, httpPort: Option[Integer], sslPort: Option[Integer])(
      block: JFunction[JBuiltInComponents, JRouter]
  ): Server = {
    val context     = JApplicationLoader.create(Environment.simple(mode = mode).asJava)
    val application = new JBuiltInComponentsFromContext(context) {
      override def router: JRouter                                         = block.apply(this)
      override def httpFilters(): java.util.List[play.mvc.EssentialFilter] = java.util.Collections.emptyList()
    }.application.asScala()
    Play.start(application)
    val serverConfig = ServerConfig(mode = mode, port = httpPort.map(_.intValue), sslPort = sslPort.map(_.intValue))
    implicitly[ServerProvider].createServer(serverConfig, application)
  }
}
