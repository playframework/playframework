/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import java.net.InetSocketAddress
import java.security.{ Provider, SecureRandom }
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.play.WebSocketHandler
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.model.headers.Expect
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.{ ConnectionContext, Http }
import akka.http.scaladsl.util.FastFuture._
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.{ Configuration, _ }
import play.api.http.{ DefaultHttpErrorHandler, HttpConfiguration, HttpErrorHandler }
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router
import play.core.{ ApplicationProvider, DefaultWebCommands, SourceMapper, WebCommands }
import play.core.server.akkahttp.{ AkkaModelConversion, HttpRequestDecoder }
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }
import play.core.server.ssl.ServerSSLEngine
import play.server.SSLEngineProvider

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

/**
 * Starts a Play server using Akka HTTP.
 */
class AkkaHttpServer(
    config: ServerConfig,
    val applicationProvider: ApplicationProvider,
    actorSystem: ActorSystem,
    materializer: Materializer,
    stopHook: () => Future[_]) extends Server {

  import AkkaHttpServer._

  assert(config.port.isDefined || config.sslPort.isDefined, "AkkaHttpServer must be given at least one of an HTTP and an HTTPS port")

  private val serverConfig = config.configuration.get[Configuration]("play.server")
  private val akkaConfig = serverConfig.get[Configuration]("akka")

  def mode = config.mode

  // Remember that some user config may not be available in development mode due to its unusual ClassLoader.
  implicit val system = actorSystem
  implicit val mat = materializer

  private def createServerBinding(port: Int, connectionContext: ConnectionContext, secure: Boolean): Http.ServerBinding = {
    // Listen for incoming connections and handle them with the `handleRequest` method.
    val initialSettings = ServerSettings(system)
    val idleTimeout = if (secure) {
      serverConfig.get[Duration]("https.idleTimeout")
    } else {
      serverConfig.get[Duration]("http.idleTimeout")
    }
    val requestTimeoutOption = akkaConfig.getOptional[Duration]("requestTimeout")

    // all akka settings that are applied to the server needs to be set here
    val serverSettings = initialSettings.withTimeouts {
      val timeouts = initialSettings.timeouts.withIdleTimeout(idleTimeout)
      requestTimeoutOption.foreach { requestTimeout => timeouts.withRequestTimeout(requestTimeout) }
      timeouts
    }
      // Play needs Raw Request Uri's to match Netty
      .withRawRequestUriHeader(true)
      .withRemoteAddressHeader(true)
      .withTransparentHeadRequests(akkaConfig.get[Boolean]("transparent-head-requests"))
      .withServerHeader(akkaConfig.getOptional[String]("server-header").filterNot(_ == "").map(headers.Server(_)))
      .withDefaultHostHeader(headers.Host(akkaConfig.get[String]("default-host-header")))

    // TODO: pass in Inet.SocketOption and LoggerAdapter params?
    val bindingFuture: Future[Http.ServerBinding] =
      Http()
        .bindAndHandleAsync(
          handler = handleRequest(_, connectionContext.isSecure),
          interface = config.address, port = port,
          connectionContext = connectionContext,
          settings = serverSettings)

    val bindTimeout = akkaConfig.get[FiniteDuration]("bindTimeout")
    Await.result(bindingFuture, bindTimeout)
  }

  private val httpServerBinding = config.port.map(port => createServerBinding(port, ConnectionContext.noEncryption(), secure = false))

  private val httpsServerBinding = config.sslPort.map { port =>
    val connectionContext = try {
      val engineProvider = ServerSSLEngine.createSSLEngineProvider(config, applicationProvider)
      // There is a mismatch between the Play SSL API and the Akka IO SSL API, Akka IO takes an SSL context, and
      // couples it with all the configuration that it will eventually pass to the created SSLEngine. Play has a
      // factory for creating an SSLEngine, so the user can configure it themselves.  However, that means that in
      // order to pass an SSLContext, we need to pass our own one that returns the SSLEngine provided by the factory.
      val sslContext = mockSslContext(engineProvider)
      ConnectionContext.https(sslContext = sslContext)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Cannot load SSL context", e)
        ConnectionContext.noEncryption()
    }
    createServerBinding(port, connectionContext, secure = true)
  }

  // Each request needs an id
  private val requestIDs = new java.util.concurrent.atomic.AtomicLong(0)

  // TODO: We can change this to an eager val when we fully support server configuration
  // instead of reading from the application configuration. At the moment we need to wait
  // until we have an Application available before we can read any configuration. :(

  private lazy val resultUtils: ServerResultUtils = {
    val httpConfiguration = applicationProvider.get match {
      case Success(app) => HttpConfiguration.fromConfiguration(app.configuration, app.environment)
      case Failure(_) => HttpConfiguration()
    }
    new ServerResultUtils(httpConfiguration)
  }

  private lazy val modelConversion: AkkaModelConversion = {
    val configuration: Option[Configuration] = applicationProvider.get.toOption.map(_.configuration)
    val forwardedHeaderHandler = new ForwardedHeaderHandler(
      ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(configuration))
    new AkkaModelConversion(resultUtils, forwardedHeaderHandler)
  }

  private def handleRequest(request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    val remoteAddress: InetSocketAddress = remoteAddressOfRequest(request)
    val decodedRequest = HttpRequestDecoder.decodeRequest(request)
    val requestId = requestIDs.incrementAndGet()
    val (convertedRequestHeader, requestBodySource) = modelConversion.convertRequest(
      requestId = requestId,
      remoteAddress = remoteAddress,
      secureProtocol = secure,
      request = decodedRequest)
    val (taggedRequestHeader, handler, newTryApp) = getHandler(convertedRequestHeader)
    val responseFuture = executeHandler(
      newTryApp,
      decodedRequest,
      taggedRequestHeader,
      requestBodySource,
      handler
    )
    responseFuture
  }

  def remoteAddressOfRequest(req: HttpRequest): InetSocketAddress =
    req.header[headers.`Remote-Address`] match {
      case Some(headers.`Remote-Address`(RemoteAddress.IP(ip, Some(port)))) =>
        new InetSocketAddress(ip, port)
      case _ => throw new IllegalStateException("`Remote-Address` header was missing")
    }

  private def getHandler(requestHeader: RequestHeader): (RequestHeader, Handler, Try[Application]) = {
    getHandlerFor(requestHeader) match {
      case Left(futureResult) =>
        (
          requestHeader,
          EssentialAction(_ => Accumulator.done(futureResult)),
          Failure(new Exception("getHandler returned Result, but not Application"))
        )
      case Right((newRequestHeader, handler, newApp)) =>
        (
          newRequestHeader,
          handler,
          Success(newApp) // TODO: Change getHandlerFor to use the app that we already had
        )
    }
  }

  private def executeHandler(
    tryApp: Try[Application],
    request: HttpRequest,
    taggedRequestHeader: RequestHeader,
    requestBodySource: Either[ByteString, Source[ByteString, _]],
    handler: Handler): Future[HttpResponse] = {

    val upgradeToWebSocket = request.header[UpgradeToWebSocket]

    // Get the app's HttpErrorHandler or fallback to a default value
    val errorHandler: HttpErrorHandler = tryApp match {
      case Success(app) => app.errorHandler
      case Failure(_) => DefaultHttpErrorHandler
    }

    (handler, upgradeToWebSocket) match {
      //execute normal action
      case (action: EssentialAction, _) =>
        val actionWithErrorHandling = EssentialAction { rh =>
          import play.core.Execution.Implicits.trampoline
          action(rh).recoverWith {
            case error => errorHandler.onServerError(taggedRequestHeader, error)
          }
        }
        executeAction(request, taggedRequestHeader, requestBodySource, actionWithErrorHandling, errorHandler)

      case (websocket: WebSocket, Some(upgrade)) =>
        import play.core.Execution.Implicits.trampoline

        websocket(taggedRequestHeader).flatMap {
          case Left(result) =>
            modelConversion.convertResult(taggedRequestHeader, result, request.protocol, errorHandler)
          case Right(flow) =>
            Future.successful(WebSocketHandler.handleWebSocket(upgrade, flow, 16384))
        }

      case (websocket: WebSocket, None) =>
        // WebSocket handler for non WebSocket request
        sys.error(s"WebSocket returned for non WebSocket request")
      case (unhandled, _) => sys.error(s"AkkaHttpServer doesn't handle Handlers of this type: $unhandled")

    }
  }

  def executeAction(
    request: HttpRequest,
    taggedRequestHeader: RequestHeader,
    requestBodySource: Either[ByteString, Source[ByteString, _]],
    action: EssentialAction,
    errorHandler: HttpErrorHandler): Future[HttpResponse] = {

    import play.core.Execution.Implicits.trampoline
    val actionAccumulator: Accumulator[ByteString, Result] = action(taggedRequestHeader)

    val source = if (request.header[Expect].contains(Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Akka-HTTP, Play signals to continue
      // by requesting demand, Akka-HTTP signals to continue by attaching a sink to the source. See
      // https://github.com/akka/akka/issues/17782 for more details.
      requestBodySource.right.map(source => Source.lazily(() => source))
    } else {
      requestBodySource
    }

    val resultFuture: Future[Result] = source match {
      case Left(bytes) if bytes.isEmpty => actionAccumulator.run()
      case Left(bytes) => actionAccumulator.run(bytes)
      case Right(s) => actionAccumulator.run(s)
    }
    val responseFuture: Future[HttpResponse] = resultFuture.fast.flatMap { result =>
      val cleanedResult: Result = resultUtils.prepareCookies(taggedRequestHeader, result)
      modelConversion.convertResult(taggedRequestHeader, cleanedResult, request.protocol, errorHandler)
    }
    responseFuture
  }

  mode match {
    case Mode.Test =>
    case _ =>
      httpServerBinding.foreach { http =>
        logger.info(s"Listening for HTTP on ${http.localAddress}")
      }
      httpsServerBinding.foreach { https =>
        logger.info(s"Listening for HTTPS on ${https.localAddress}")
      }
  }

  override def stop() {

    mode match {
      case Mode.Test =>
      case _ => logger.info("Stopping server...")
    }

    // First, stop listening
    def unbind(binding: Http.ServerBinding) = Await.result(binding.unbind(), Duration.Inf)
    httpServerBinding.foreach(unbind)
    httpsServerBinding.foreach(unbind)
    applicationProvider.current.foreach(Play.stop)

    try {
      super.stop()
    } catch {
      case NonFatal(e) => logger.error("Error while stopping logger", e)
    }

    system.terminate()

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    Await.result(stopHook(), Duration.Inf)
  }

  override lazy val mainAddress: InetSocketAddress = {
    httpServerBinding.orElse(httpsServerBinding).map(_.localAddress).get
  }

  def httpPort = httpServerBinding.map(_.localAddress.getPort)

  def httpsPort = httpsServerBinding.map(_.localAddress.getPort)

  /**
   * There is a mismatch between the Play SSL API and the Akka IO SSL API, Akka IO takes an SSL context, and
   * couples it with all the configuration that it will eventually pass to the created SSLEngine. Play has a
   * factory for creating an SSLEngine, so the user can configure it themselves.  However, that means that in
   * order to pass an SSLContext, we need to implement our own mock one that delegates to the SSLEngineProvider
   * when creating an SSLEngine.
   */
  private def mockSslContext(sslEngineProvider: SSLEngineProvider): SSLContext = {
    new SSLContext(new SSLContextSpi() {
      def engineCreateSSLEngine() = sslEngineProvider.createSSLEngine()
      def engineCreateSSLEngine(s: String, i: Int) = engineCreateSSLEngine()

      def engineInit(keyManagers: Array[KeyManager], trustManagers: Array[TrustManager], secureRandom: SecureRandom) = ()
      def engineGetClientSessionContext() = SSLContext.getDefault.getClientSessionContext
      def engineGetServerSessionContext() = SSLContext.getDefault.getServerSessionContext
      def engineGetSocketFactory() = SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]
      def engineGetServerSocketFactory() = SSLServerSocketFactory.getDefault.asInstanceOf[SSLServerSocketFactory]
    }, new Provider("Play SSlEngineProvider delegate", 1d,
      "A provider that only implements the creation of SSL engines, and delegates to Play's SSLEngineProvider") {},
      "Play SSLEngineProvider delegate") {
    }

  }
}

object AkkaHttpServer {

  private val logger = Logger(classOf[AkkaHttpServer])

  /**
   * A ServerProvider for creating an AkkaHttpServer.
   */
  implicit val provider: AkkaHttpServerProvider = new AkkaHttpServerProvider

  /**
   * Create a Netty server from the given application and server configuration.
   *
   * @param application The application.
   * @param config The server configuration.
   * @return A started Netty server, serving the application.
   */
  def fromApplication(application: Application, config: ServerConfig = ServerConfig()): AkkaHttpServer = {
    new AkkaHttpServer(config, ApplicationProvider(application), application.actorSystem,
      application.materializer, () => Future.successful(()))
  }

  def fromRouter(config: ServerConfig = ServerConfig())(routes: PartialFunction[RequestHeader, Handler]): AkkaHttpServer = {
    new AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents {
      override lazy val serverConfig: ServerConfig = config
      lazy val router: Router = Router.from(routes)
    }.server
  }
}

/**
 * Knows how to create an AkkaHttpServer.
 */
class AkkaHttpServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context) =
    new AkkaHttpServer(context.config, context.appProvider, context.actorSystem, context.materializer,
      context.stopHook)
}

trait AkkaHttpServerComponents {
  lazy val serverConfig: ServerConfig = ServerConfig()
  lazy val server: AkkaHttpServer = {
    // Start the application first
    Play.start(application)
    new AkkaHttpServer(serverConfig, ApplicationProvider(application), application.actorSystem,
      application.materializer, serverStopHook)
  }

  lazy val environment: Environment = Environment.simple(mode = serverConfig.mode)
  lazy val sourceMapper: Option[SourceMapper] = None
  lazy val webCommands: WebCommands = new DefaultWebCommands
  lazy val configuration: Configuration = Configuration(ConfigFactory.load())
  lazy val applicationLifecycle: DefaultApplicationLifecycle = new DefaultApplicationLifecycle

  def application: Application

  /**
   * Called when Server.stop is called.
   */
  def serverStopHook: () => Future[Unit] = () => Future.successful(())
}
