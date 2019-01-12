/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.InetSocketAddress
import java.security.{ Provider, SecureRandom }

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.annotation.ApiMayChange
import akka.http.play.WebSocketHandler
import akka.http.scaladsl.model.headers.Expect
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.model.{ headers, _ }
import akka.http.scaladsl.settings.{ ParserSettings, ServerSettings }
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.{ ConnectionContext, Http, HttpConnectionContext }
import akka.http.scaladsl.UseHttp2._
import akka.stream.{ Materializer, TLSClientAuth }
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.{ Config, ConfigMemorySize }
import javax.net.ssl._
import play.api._
import play.api.http.{ DefaultHttpErrorHandler, HeaderNames, HttpErrorHandler, Status }
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.mvc.akkahttp.AkkaHttpHandler
import play.api.routing.Router
import play.core.ApplicationProvider
import play.core.server.Server.ServerStoppedReason
import play.core.server.akkahttp.{ AkkaModelConversion, HttpRequestDecoder }
import play.core.server.common.{ ReloadCache, ServerDebugInfo, ServerResultUtils }
import play.core.server.ssl.ServerSSLEngine

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

/**
 * Starts a Play server using Akka HTTP.
 */
class AkkaHttpServer(context: AkkaHttpServer.Context) extends Server {

  registerShutdownTasks()

  @deprecated("Use new AkkaHttpServer(Context) instead", "2.6.14")
  def this(config: ServerConfig, applicationProvider: ApplicationProvider, actorSystem: ActorSystem, materializer: Materializer, stopHook: () => Future[_]) =
    this(AkkaHttpServer.Context(config, applicationProvider, actorSystem, materializer, stopHook))

  import AkkaHttpServer._

  assert(context.config.port.isDefined || context.config.sslPort.isDefined, "AkkaHttpServer must be given at least one of an HTTP and an HTTPS port")

  /** Helper to access server configuration under the `play.server` prefix. */
  private val serverConfig = context.config.configuration.get[Configuration]("play.server")
  /** Helper to access server configuration under the `play.server.akka` prefix. */
  private val akkaServerConfig = context.config.configuration.get[Configuration]("play.server.akka")

  override def mode: Mode = context.config.mode
  override def applicationProvider: ApplicationProvider = context.appProvider

  // Remember that some user config may not be available in development mode due to its unusual ClassLoader.
  implicit private val system: ActorSystem = context.actorSystem
  implicit private val mat: Materializer = context.materializer

  private val http2Enabled: Boolean = akkaServerConfig.getOptional[Boolean]("http2.enabled") getOrElse false

  @ApiMayChange
  private val http2AlwaysForInsecure: Boolean = http2Enabled && (akkaServerConfig.getOptional[Boolean]("http2.alwaysForInsecure") getOrElse false)

  /**
   * Play's configuration for the Akka HTTP server. Initialized by a call to [[createAkkaHttpConfig()]].
   *
   * Note that the rest of the [[ActorSystem]] outside Akka HTTP is initialized by the configuration in [[context.config]].
   */
  protected val akkaHttpConfig: Config = createAkkaHttpConfig()

  /**
   * Creates the configuration used to initialize the Akka HTTP subsystem. By default this uses the ActorSystem's
   * configuration, with an additional setting patched in to enable or disable HTTP/2.
   */
  protected def createAkkaHttpConfig(): Config = {
    (Configuration(system.settings.config) ++ Configuration(
      "akka.http.server.preview.enable-http2" -> http2Enabled
    )).underlying
  }

  /**
   * Parses the config setting `infinite` as `Long.MaxValue` otherwise uses Config's built-in
   * parsing of byte values.
   */
  private def getPossiblyInfiniteBytes(config: Config, path: String): Long = {
    config.getString(path) match {
      case "infinite" => Long.MaxValue
      case _ => config.getBytes(path)
    }
  }

  /** Play's parser settings for Akka HTTP. Initialized by a call to [[createParserSettings()]]. */
  protected val parserSettings: ParserSettings = createParserSettings()

  /** Called by Play when creating its Akka HTTP parser settings. Result stored in [[parserSettings]]. */
  protected def createParserSettings(): ParserSettings = ParserSettings(akkaHttpConfig)
    .withMaxContentLength(getPossiblyInfiniteBytes(akkaServerConfig.underlying, "max-content-length"))
    .withMaxHeaderValueLength(akkaServerConfig.get[ConfigMemorySize]("max-header-value-length").toBytes.toInt)
    .withIncludeTlsSessionInfoHeader(akkaServerConfig.get[Boolean]("tls-session-info-header"))
    .withModeledHeaderParsing(false) // Disable most of Akka HTTP's header parsing; use RawHeaders instead

  /**
   * Create Akka HTTP settings for a given port binding.
   *
   * Called by Play when binding a handler to a server port. Will be called once per port. Called by the
   * [[createServerBinding()]] method.
   */
  protected def createServerSettings(port: Int, connectionContext: ConnectionContext, secure: Boolean): ServerSettings = {
    val idleTimeout = serverConfig.get[Duration](if (secure) "https.idleTimeout" else "http.idleTimeout")
    val requestTimeout = akkaServerConfig.get[Duration]("requestTimeout")
    val initialSettings = ServerSettings(akkaHttpConfig)
    initialSettings
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
  }

  /**
   * Bind Akka HTTP to a port to listen for incoming connections. Calls [[createServerSettings()]] to configure the
   * binding and [[handleRequest()]] as a handler for the binding.
   */
  private def createServerBinding(port: Int, connectionContext: ConnectionContext, secure: Boolean): Http.ServerBinding = {
    // TODO: pass in Inet.SocketOption and LoggerAdapter params?
    val bindingFuture: Future[Http.ServerBinding] = try {
      Http()
        .bindAndHandleAsync(
          handler = handleRequest(_, connectionContext.isSecure),
          interface = context.config.address, port = port,
          connectionContext = connectionContext,
          settings = createServerSettings(port, connectionContext, secure))
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

  private val httpServerBinding = context.config.port.map(port => createServerBinding(port, HttpConnectionContext(http2 = if (http2AlwaysForInsecure) Always else Never), secure = false))

  private val httpsServerBinding = context.config.sslPort.map { port =>
    val connectionContext = try {
      // There is a mismatch between the Play SSL API and the Akka IO SSL API, Akka IO takes an SSL context, and
      // couples it with all the configuration that it will eventually pass to the created SSLEngine. Play has a
      // factory for creating an SSLEngine, so the user can configure it themselves.  However, that means that in
      // order to pass an SSLContext, we need to pass our own one that returns the SSLEngine provided by the factory.
      val sslContext = mockSslContext()

      val clientAuth: Option[TLSClientAuth] = createClientAuth()

      ConnectionContext.https(
        sslContext = sslContext,
        clientAuth = clientAuth
      )
    } catch {
      case NonFatal(e) =>
        logger.error(s"Cannot load SSL context", e)
        ConnectionContext.noEncryption()
    }
    createServerBinding(port, connectionContext, secure = true)
  }

  /** Creates AkkaHttp TLSClientAuth */
  protected def createClientAuth(): Option[TLSClientAuth] = {

    // Need has precedence over Want, hence the if/else if
    if (serverConfig.get[Boolean]("https.needClientAuth")) {
      Some(TLSClientAuth.need)
    } else if (serverConfig.get[Boolean]("https.wantClientAuth")) {
      Some(TLSClientAuth.want)
    } else {
      None
    }
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
      modelConversion: AkkaModelConversion,
      serverDebugInfo: Option[ServerDebugInfo]
  )

  /**
   * A helper to cache values that are derived from the current application.
   */
  private val reloadCache = new ReloadCache[ReloadCacheValues] {
    override protected def reloadValue(tryApp: Try[Application]): ReloadCacheValues = {
      val serverResultUtils = reloadServerResultUtils(tryApp)
      val forwardedHeaderHandler = reloadForwardedHeaderHandler(tryApp)
      val illegalResponseHeaderValue = ParserSettings.IllegalResponseHeaderValueProcessingMode(akkaServerConfig.get[String]("illegal-response-header-value-processing-mode"))
      val modelConversion = new AkkaModelConversion(
        serverResultUtils,
        forwardedHeaderHandler,
        illegalResponseHeaderValue)
      ReloadCacheValues(
        resultUtils = serverResultUtils,
        modelConversion = modelConversion,
        serverDebugInfo = reloadDebugInfo(tryApp, provider)
      )
    }
  }

  private def resultUtils(tryApp: Try[Application]): ServerResultUtils =
    reloadCache.cachedFrom(tryApp).resultUtils
  private def modelConversion(tryApp: Try[Application]): AkkaModelConversion =
    reloadCache.cachedFrom(tryApp).modelConversion

  private def handleRequest(request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    val decodedRequest = HttpRequestDecoder.decodeRequest(request)
    val tryApp = applicationProvider.get
    val (convertedRequestHeader, requestBodySource): (RequestHeader, Either[ByteString, Source[ByteString, Any]]) = {
      val remoteAddress: InetSocketAddress = remoteAddressOfRequest(request)
      val requestId: Long = requestIDs.incrementAndGet()
      modelConversion(tryApp).convertRequest(
        requestId = requestId,
        remoteAddress = remoteAddress,
        secureProtocol = secure,
        request = decodedRequest)
    }
    val debugInfoRequestHeader: RequestHeader = {
      val debugInfo: Option[ServerDebugInfo] = reloadCache.cachedFrom(tryApp).serverDebugInfo
      ServerDebugInfo.attachToRequestHeader(convertedRequestHeader, debugInfo)
    }
    val (taggedRequestHeader, handler) = Server.getHandlerFor(debugInfoRequestHeader, tryApp)
    val responseFuture = executeHandler(
      tryApp,
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
      case Failure(_) => system.dispatcher
    }

    (handler, upgradeToWebSocket) match {
      //execute normal action
      case (action: EssentialAction, _) =>
        runAction(tryApp, request, taggedRequestHeader, requestBodySource, action, errorHandler)
      case (websocket: WebSocket, Some(upgrade)) =>
        val bufferLimit = context.config.configuration.getDeprecated[ConfigMemorySize]("play.server.websocket.frame.maxLength", "play.websocket.buffer.limit").toBytes.toInt

        websocket(taggedRequestHeader).fast.flatMap {
          case Left(result) =>
            modelConversion(tryApp).convertResult(taggedRequestHeader, result, request.protocol, errorHandler)
          case Right(flow) =>
            // For now, like Netty, select an arbitrary subprotocol from the list of subprotocols proposed by the client
            // Eventually it would be better to allow the handler to specify the protocol it selected
            // See also https://github.com/playframework/playframework/issues/7895
            val selectedSubprotocol = upgrade.requestedProtocols.headOption
            Future.successful(WebSocketHandler.handleWebSocket(upgrade, flow, bufferLimit, selectedSubprotocol))
        }

      case (websocket: WebSocket, None) =>
        // WebSocket handler for non WebSocket request
        logger.trace(s"Bad websocket request: $request")
        val action = EssentialAction(_ => Accumulator.done(
          Results.Status(Status.UPGRADE_REQUIRED)("Upgrade to WebSocket required").withHeaders(
            HeaderNames.UPGRADE -> "websocket",
            HeaderNames.CONNECTION -> HeaderNames.UPGRADE
          )
        ))
        runAction(tryApp, request, taggedRequestHeader, requestBodySource, action, errorHandler)
      case (akkaHttpHandler: AkkaHttpHandler, _) =>
        akkaHttpHandler(request)
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
      action, errorHandler)(system.dispatcher)
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

  override def stop(): Unit = CoordinatedShutdownSupport.syncShutdown(context.actorSystem, ServerStoppedReason)

  // Using CoordinatedShutdown means that instead of invoking code imperatively in `stop`
  // we have to register it as early as possible as CoordinatedShutdown tasks and
  // then `stop` runs CoordinatedShutdown.
  private def registerShutdownTasks(): Unit = {

    implicit val exCtx: ExecutionContext = context.actorSystem.dispatcher

    // Register all shutdown tasks
    val cs = CoordinatedShutdown(context.actorSystem)
    cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "trace-server-stop-request") {
      () =>
        mode match {
          case Mode.Test =>
          case _ => logger.info("Stopping server...")
        }
        Future.successful(Done)
    }

    // Stop listening.
    // TODO: this can be improved so unbind is deferred until `service-stop`. We could
    // respond 503 in the meantime.
    cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "akka-http-server-unbind") {
      () =>
        def unbind(binding: Option[Http.ServerBinding]): Future[Done] =
          binding.map(_.unbind()).getOrElse(Future.successful(Done))

        for {
          _ <- unbind(httpServerBinding)
          _ <- unbind(httpsServerBinding)
        } yield Done
    }

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "user-provided-server-stop-hook") {
      () => context.stopHook().map(_ => Done)
    }
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "shutdown-logger") {
      () =>
        Future {
          super.stop()
          Done
        }
    }

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
  private def mockSslContext(): SSLContext = {
    new SSLContext(new SSLContextSpi() {
      private lazy val sslEngineProvider = ServerSSLEngine.createSSLEngineProvider(context.config, applicationProvider)
      override def engineCreateSSLEngine(): SSLEngine = sslEngineProvider.createSSLEngine()
      override def engineCreateSSLEngine(s: String, i: Int): SSLEngine = engineCreateSSLEngine()

      override def engineInit(keyManagers: Array[KeyManager], trustManagers: Array[TrustManager], secureRandom: SecureRandom): Unit = ()
      override def engineGetClientSessionContext(): SSLSessionContext = SSLContext.getDefault.getClientSessionContext
      override def engineGetServerSessionContext(): SSLSessionContext = SSLContext.getDefault.getServerSessionContext
      override def engineGetSocketFactory(): SSLSocketFactory = SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]
      override def engineGetServerSocketFactory(): SSLServerSocketFactory = SSLServerSocketFactory.getDefault.asInstanceOf[SSLServerSocketFactory]
    }, new Provider("Play SSlEngineProvider delegate", 1d,
      "A provider that only implements the creation of SSL engines, and delegates to Play's SSLEngineProvider") {},
      "Play SSLEngineProvider delegate") {
    }

  }
}

/**
 * Creates an AkkaHttpServer from a given router using [[BuiltInComponents]]:
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
   * The values needed to initialize an [[AkkaHttpServer]].
   *
   * @param config Basic server configuration values.
   * @param appProvider An object which can be queried to get an Application.
   * @param actorSystem An ActorSystem that the server can use.
   * @param stopHook A function that should be called by the server when it stops.
   * This function can be used to close resources that are provided to the server.
   */
  final case class Context(
      config: ServerConfig,
      appProvider: ApplicationProvider,
      actorSystem: ActorSystem,
      materializer: Materializer,
      stopHook: () => Future[_])

  object Context {

    /**
     * Create a `Context` object from several common components.
     */
    def fromComponents(
      serverConfig: ServerConfig,
      application: Application,
      stopHook: () => Future[_] = () => Future.successful(())): Context =
      AkkaHttpServer.Context(
        config = serverConfig,
        appProvider = ApplicationProvider(application),
        actorSystem = application.actorSystem,
        materializer = application.materializer,
        stopHook = stopHook
      )

    /**
     * Create a `Context` object from a `ServerProvider.Context`.
     */
    def fromServerProviderContext(serverProviderContext: ServerProvider.Context): Context = {
      import serverProviderContext._
      AkkaHttpServer.Context(config, appProvider, actorSystem, materializer, stopHook)
    }
  }

  /**
   * A ServerProvider for creating an AkkaHttpServer.
   */
  implicit val provider: AkkaHttpServerProvider = new AkkaHttpServerProvider

  /**
   * Create a Akka HTTP server from the given application and server configuration.
   *
   * @param application The application.
   * @param config The server configuration.
   * @return A started Akka HTTP server, serving the application.
   */
  def fromApplication(application: Application, config: ServerConfig = ServerConfig()): AkkaHttpServer = {
    new AkkaHttpServer(Context.fromComponents(config, application))
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
  def createServer(context: ServerProvider.Context) = {
    new AkkaHttpServer(AkkaHttpServer.Context.fromServerProviderContext(context))
  }
}

/**
 * Components for building a simple Akka HTTP Server.
 */
trait AkkaHttpServerComponents extends ServerComponents {
  lazy val server: AkkaHttpServer = {
    // Start the application first
    Play.start(application)
    new AkkaHttpServer(AkkaHttpServer.Context.fromComponents(serverConfig, application, serverStopHook))
  }

  def application: Application
}

/**
 * A convenient helper trait for constructing an AkkaHttpServer, for example:
 *
 * {{{
 *   val components = new DefaultAkkaHttpServerComponents {
 *     override lazy val router = {
 *       case GET(p"/") => Action(parse.json) { body =>
 *         Ok("Hello")
 *       }
 *     }
 *   }
 *   val server = components.server
 * }}}
 */
trait DefaultAkkaHttpServerComponents
  extends AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents
