/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import java.net.InetSocketAddress
import java.security.{ Provider, SecureRandom }
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.play.WebSocketHandler
import akka.http.scaladsl.model.{ headers, _ }
import akka.http.scaladsl.model.headers.Expect
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.settings.{ ParserSettings, ServerSettings }
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.{ ConnectionContext, Http }
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.{ Config, ConfigMemorySize }
import play.api._
import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router
import play.core.server.akkahttp.{ AkkaModelConversion, HttpRequestDecoder }
import play.core.server.common.{ ReloadCache, ServerResultUtils }
import play.core.server.ssl.ServerSSLEngine
import play.core.ApplicationProvider
import play.server.SSLEngineProvider

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
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
  private val akkaServerConfig = config.configuration.get[Configuration]("play.server.akka")

  override def mode: Mode = config.mode

  // Remember that some user config may not be available in development mode due to its unusual ClassLoader.
  implicit private val system: ActorSystem = actorSystem
  implicit private val mat: Materializer = materializer

  private val http2Enabled: Boolean = akkaServerConfig.getOptional[Boolean]("http2.enabled") getOrElse false

  private def getPossiblyInfiniteBytes(config: Config, path: String): Long = {
    config.getString(path) match {
      case "infinite" => Long.MaxValue
      case x => config.getBytes(path)
    }
  }

  private def createServerBinding(port: Int, connectionContext: ConnectionContext, secure: Boolean): Http.ServerBinding = {
    // Listen for incoming connections and handle them with the `handleRequest` method.

    val initialConfig = (Configuration(system.settings.config) ++ Configuration(
      "akka.http.server.preview.enable-http2" -> http2Enabled
    )).underlying

    val parserSettings = ParserSettings(initialConfig)
      .withMaxContentLength(getPossiblyInfiniteBytes(akkaServerConfig.underlying, "max-content-length"))
      .withIncludeTlsSessionInfoHeader(akkaServerConfig.get[Boolean]("tls-session-info-header"))

    val initialSettings = ServerSettings(initialConfig)

    val idleTimeout = serverConfig.get[Duration](if (secure) "https.idleTimeout" else "http.idleTimeout")
    val requestTimeout = akkaServerConfig.get[Duration]("requestTimeout")

    // all akka settings that are applied to the server needs to be set here
    val serverSettings: ServerSettings = initialSettings
      .withTimeouts(
        initialSettings.timeouts
          .withIdleTimeout(idleTimeout)
          .withRequestTimeout(requestTimeout)
      )
      // Play needs these headers to fill in fields in its request model
      .withRawRequestUriHeader(true)
      .withRemoteAddressHeader(true)
      // Disable Akka-HTTP's transparent HEAD handling. so that play's HEAD handling can take action
      .withTransparentHeadRequests(akkaServerConfig.get[Boolean]("transparent-head-requests"))
      .withServerHeader(akkaServerConfig.get[Option[String]]("server-header").collect { case s if s.nonEmpty => headers.Server(s) })
      .withDefaultHostHeader(headers.Host(akkaServerConfig.get[String]("default-host-header")))
      .withParserSettings(parserSettings)

    // TODO: pass in Inet.SocketOption and LoggerAdapter params?
    val bindingFuture: Future[Http.ServerBinding] = try {
      Http()
        .bindAndHandleAsync(
          handler = handleRequest(_, connectionContext.isSecure),
          interface = config.address, port = port,
          connectionContext = connectionContext,
          settings = serverSettings)
    } catch {
      // Http2SupportNotPresentException is private[akka] so we need to match the name
      case e: Throwable if e.getClass.getSimpleName == "Http2SupportNotPresentException" =>
        throw new RuntimeException(
          "HTTP/2 enabled but akka-http2-support not found. " +
            "Add .enablePlugins(PlayAkkaHttp2Support) in build.sbt", e)
    }

    val bindTimeout = akkaServerConfig.get[FiniteDuration]("bindTimeout")
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

  if (http2Enabled) {
    logger.info(s"Enabling HTTP/2 on Akka HTTP server...")
    if (httpsServerBinding.isEmpty) {
      val logMessage = s"No HTTPS server bound. Only binding HTTP. Many user agents only support HTTP/2 over HTTPS."
      // warn in dev/test mode, since we are likely accessing the server directly, but debug otherwise
      mode match {
        case Mode.Dev | Mode.Test => logger.warn(logMessage)
        case _ => logger.debug(logMessage)
      }
    }
  }

  // Each request needs an id
  private val requestIDs = new java.util.concurrent.atomic.AtomicLong(0)

  /**
   * Values that are cached based on the current application.
   */
  private case class ReloadCacheValues(
      resultUtils: ServerResultUtils,
      modelConversion: AkkaModelConversion
  )

  /**
   * A helper to cache values that are derived from the current application.
   */
  private val reloadCache = new ReloadCache[ReloadCacheValues] {
    override protected def reloadValue(tryApp: Try[Application]): ReloadCacheValues = {
      val serverResultUtils = reloadServerResultUtils(tryApp)
      val forwardedHeaderHandler = reloadForwardedHeaderHandler(tryApp)
      val illegalResponseHeaderValue = ParserSettings.IllegalResponseHeaderValueProcessingMode(akkaServerConfig.get[String]("illegal-response-header-value-processing-mode"))
      val modelConversion = new AkkaModelConversion(serverResultUtils, forwardedHeaderHandler, illegalResponseHeaderValue)
      ReloadCacheValues(
        resultUtils = serverResultUtils,
        modelConversion = modelConversion
      )
    }
  }

  private def resultUtils(tryApp: Try[Application]): ServerResultUtils =
    reloadCache.cachedFrom(tryApp).resultUtils
  private def modelConversion(tryApp: Try[Application]): AkkaModelConversion =
    reloadCache.cachedFrom(tryApp).modelConversion

  private def handleRequest(request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    val remoteAddress: InetSocketAddress = remoteAddressOfRequest(request)
    val decodedRequest = HttpRequestDecoder.decodeRequest(request)
    val requestId = requestIDs.incrementAndGet()
    val tryApp = applicationProvider.get
    val (convertedRequestHeader, requestBodySource) = modelConversion(tryApp).convertRequest(
      requestId = requestId,
      remoteAddress = remoteAddress,
      secureProtocol = secure,
      request = decodedRequest)
    val (taggedRequestHeader, handler, newTryApp) = getHandler(convertedRequestHeader, tryApp)
    val responseFuture = executeHandler(
      newTryApp,
      decodedRequest,
      taggedRequestHeader,
      requestBodySource,
      handler
    )
    responseFuture
  }

  def remoteAddressOfRequest(req: HttpRequest): InetSocketAddress = {
    req.header[headers.`Remote-Address`] match {
      case Some(headers.`Remote-Address`(RemoteAddress.IP(ip, Some(port)))) =>
        new InetSocketAddress(ip, port)
      case _ => throw new IllegalStateException("`Remote-Address` header was missing")
    }
  }

  private def getHandler(
    requestHeader: RequestHeader, tryApp: Try[Application]
  ): (RequestHeader, Handler, Try[Application]) = {
    Server.getHandlerFor(requestHeader, tryApp) match {
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

    // default execution context used for executing the action
    implicit val defaultExecutionContext: ExecutionContext = tryApp match {
      case Success(app) => app.actorSystem.dispatcher
      case Failure(_) => actorSystem.dispatcher
    }

    (handler, upgradeToWebSocket) match {
      //execute normal action
      case (action: EssentialAction, _) =>
        runAction(tryApp, request, taggedRequestHeader, requestBodySource, action, errorHandler)
      case (websocket: WebSocket, Some(upgrade)) =>
        val bufferLimit = config.configuration.getDeprecated[ConfigMemorySize]("play.server.websocket.frame.maxLength", "play.websocket.buffer.limit").toBytes.toInt

        websocket(taggedRequestHeader).fast.flatMap {
          case Left(result) =>
            modelConversion(tryApp).convertResult(taggedRequestHeader, result, request.protocol, errorHandler)
          case Right(flow) =>
            Future.successful(WebSocketHandler.handleWebSocket(upgrade, flow, bufferLimit))
        }

      case (websocket: WebSocket, None) =>
        // WebSocket handler for non WebSocket request
        sys.error(s"WebSocket returned for non WebSocket request")
      case (unhandled, _) => sys.error(s"AkkaHttpServer doesn't handle Handlers of this type: $unhandled")

    }
  }

  @deprecated("This method is an internal API and should not be public", "2.6.10")
  def executeAction(
    request: HttpRequest,
    taggedRequestHeader: RequestHeader,
    requestBodySource: Either[ByteString, Source[ByteString, _]],
    action: EssentialAction,
    errorHandler: HttpErrorHandler): Future[HttpResponse] = {
    runAction(applicationProvider.get, request, taggedRequestHeader, requestBodySource,
      action, errorHandler)(actorSystem.dispatcher)
  }

  private[play] def runAction(
    tryApp: Try[Application],
    request: HttpRequest,
    taggedRequestHeader: RequestHeader,
    requestBodySource: Either[ByteString, Source[ByteString, _]],
    action: EssentialAction,
    errorHandler: HttpErrorHandler)(implicit ec: ExecutionContext): Future[HttpResponse] = {

    val futureAcc: Future[Accumulator[ByteString, Result]] = Future(action(taggedRequestHeader))

    val source = if (request.header[Expect].contains(Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Akka-HTTP, Play signals to continue
      // by requesting demand, Akka-HTTP signals to continue by attaching a sink to the source. See
      // https://github.com/akka/akka/issues/17782 for more details.
      requestBodySource.right.map(source => Source.lazily(() => source))
    } else {
      requestBodySource
    }

    // here we use FastFuture so the flatMap shouldn't actually need the executionContext
    val resultFuture: Future[Result] = futureAcc.fast.flatMap { actionAccumulator =>
      source match {
        case Left(bytes) if bytes.isEmpty => actionAccumulator.run()
        case Left(bytes) => actionAccumulator.run(bytes)
        case Right(s) => actionAccumulator.run(s)
      }
    }.recoverWith {
      case e: Throwable =>
        errorHandler.onServerError(taggedRequestHeader, e)
    }
    val responseFuture: Future[HttpResponse] = resultFuture.flatMap { result =>
      val cleanedResult: Result = resultUtils(tryApp).prepareCookies(taggedRequestHeader, result)
      modelConversion(tryApp).convertResult(taggedRequestHeader, cleanedResult, request.protocol, errorHandler)
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

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    Await.result(stopHook(), Duration.Inf)
  }

  override lazy val mainAddress: InetSocketAddress = {
    httpServerBinding.orElse(httpsServerBinding).map(_.localAddress).get
  }

  override def httpPort: Option[Int] = httpServerBinding.map(_.localAddress.getPort)

  override def httpsPort: Option[Int] = httpsServerBinding.map(_.localAddress.getPort)

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

/**
 * Creates an AkkaHttpServer from the given router:
 *
 * {{{
 *   val server = AkkaHttpServer.fromRouter(ServerConfig(port = Some(9002))) {
 *     case GET(p"/") => Action {
 *       Results.Ok("Hello")
 *     }
 *   }
 * }}}
 *
 * Or from a given router using [[BuiltInComponents]]:
 *
 * {{{
 *   val server = AkkaHttpServer.fromRouterWithComponents(ServerConfig(port = Some(9002))) { components =>
 *     import play.api.mvc.Results._
 *     import components.{ defaultActionBuilder => Action }
 *     {
 *       case GET(p"/") => Action {
 *         Ok("Hello")
 *       }
 *     }
 *   }
 * }}}
 *
 * Use this together with <a href="https://www.playframework.com/documentation/2.6.x/ScalaSirdRouter">Sird Router</a>.
 */
object AkkaHttpServer extends ServerFromRouter {

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

  override protected def createServerFromRouter(serverConf: ServerConfig = ServerConfig())(routes: ServerComponents with BuiltInComponents => Router): Server = {
    new AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents {
      override lazy val serverConfig: ServerConfig = serverConf
      override def router: Router = routes(this)
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

/**
 * Components for building a simple Akka HTTP Server.
 */
trait AkkaHttpServerComponents extends ServerComponents {
  lazy val server: AkkaHttpServer = {
    // Start the application first
    Play.start(application)
    new AkkaHttpServer(serverConfig, ApplicationProvider(application), application.actorSystem,
      application.materializer, serverStopHook)
  }

  def application: Application
}
