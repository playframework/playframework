/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.InetSocketAddress
import javax.net.ssl._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.http.play.WebSocketHandler
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.model.headers.Expect
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.settings.ParserSettings
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpConnectionContext
import akka.stream.scaladsl._
import akka.stream.Materializer
import akka.stream.TLSClientAuth
import akka.util.ByteString
import akka.Done
import com.typesafe.config.Config
import com.typesafe.config.ConfigMemorySize
import play.api._
import play.api.http.{ HttpProtocol => PlayHttpProtocol }
import play.api.http.DefaultHttpErrorHandler
import play.api.http.DevHttpErrorHandler
import play.api.http.HeaderNames
import play.api.http.HttpErrorHandler
import play.api.http.HttpErrorInfo
import play.api.http.Status
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.mvc.akkahttp.AkkaHttpHandler
import play.api.mvc.request.RequestAttrKey
import play.api.routing.Router
import play.core.server.akkahttp.AkkaModelConversion
import play.core.server.akkahttp.AkkaServerConfigReader
import play.core.server.akkahttp.HttpRequestDecoder
import play.core.server.common.ReloadCache
import play.core.server.common.ServerDebugInfo
import play.core.server.common.ServerResultUtils
import play.core.server.ssl.ServerSSLEngine
import play.core.server.Server.ServerStoppedReason
import play.core.ApplicationProvider

/**
 * Starts a Play server using Akka HTTP.
 */
class AkkaHttpServer(context: AkkaHttpServer.Context) extends Server {

  import AkkaHttpServer._

  assert(
    context.config.port.isDefined || context.config.sslPort.isDefined,
    "AkkaHttpServer must be given at least one of an HTTP and an HTTPS port"
  )

  override def mode: Mode                               = context.config.mode
  override def applicationProvider: ApplicationProvider = context.appProvider

  /** Helper to access server configuration under the `play.server` prefix. */
  private val serverConfig = context.config.configuration.get[Configuration]("play.server")

  /** Helper to access server configuration under the `play.server.akka` prefix. */
  private val akkaServerConfig = serverConfig.get[Configuration]("akka")

  private val akkaServerConfigReader = new AkkaServerConfigReader(akkaServerConfig)

  private lazy val initialSettings = ServerSettings(akkaHttpConfig)

  private val httpIdleTimeout  = serverConfig.get[Duration]("http.idleTimeout")
  private val httpsIdleTimeout = serverConfig.get[Duration]("https.idleTimeout")
  private val requestTimeout   = akkaServerConfig.get[Duration]("requestTimeout")
  private val bindTimeout      = akkaServerConfig.get[FiniteDuration]("bindTimeout")
  private val terminationDelay = serverConfig.get[FiniteDuration]("waitBeforeTermination")
  private val terminationTimeout =
    serverConfig.getDeprecated[Option[FiniteDuration]]("terminationTimeout", "akka.terminationTimeout")

  private val maxContentLength =
    Server.getPossiblyInfiniteBytes(serverConfig.underlying, "max-content-length", "akka.max-content-length")
  private val maxHeaderValueLength =
    serverConfig.getDeprecated[ConfigMemorySize]("max-header-size", "akka.max-header-value-length").toBytes.toInt
  private val includeTlsSessionInfoHeader = akkaServerConfig.get[Boolean]("tls-session-info-header")
  private val defaultHostHeader           = akkaServerConfigReader.getHostHeader.fold(throw _, identity)
  private val transparentHeadRequests     = akkaServerConfig.get[Boolean]("transparent-head-requests")
  private val serverHeaderConfig          = akkaServerConfig.getOptional[String]("server-header")
  private val pipeliningLimit             = akkaServerConfig.get[Int]("pipelining-limit")
  private val serverHeader = serverHeaderConfig.collect {
    case s if s.nonEmpty => headers.Server(s)
  }

  private val httpsNeedClientAuth = serverConfig.get[Boolean]("https.needClientAuth")
  private val httpsWantClientAuth = serverConfig.get[Boolean]("https.wantClientAuth")
  private val illegalResponseHeaderValueProcessingMode =
    akkaServerConfig.get[String]("illegal-response-header-value-processing-mode")
  private val wsBufferLimit      = serverConfig.get[ConfigMemorySize]("websocket.frame.maxLength").toBytes.toInt
  private val wsKeepAliveMode    = serverConfig.get[String]("websocket.periodic-keep-alive-mode")
  private val wsKeepAliveMaxIdle = serverConfig.get[Duration]("websocket.periodic-keep-alive-max-idle")

  private val http2Enabled: Boolean = akkaServerConfig.getOptional[Boolean]("http2.enabled").getOrElse(false)

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
  protected def createAkkaHttpConfig(): Config =
    Configuration("akka.http.server.preview.enable-http2" -> http2Enabled)
      .withFallback(Configuration(context.actorSystem.settings.config))
      .underlying

  /** Play's parser settings for Akka HTTP. Initialized by a call to [[createParserSettings()]]. */
  protected val parserSettings: ParserSettings = createParserSettings()

  /** Called by Play when creating its Akka HTTP parser settings. Result stored in [[parserSettings]]. */
  protected def createParserSettings(): ParserSettings =
    ParserSettings(akkaHttpConfig)
      .withMaxContentLength(maxContentLength)
      .withMaxHeaderValueLength(maxHeaderValueLength)
      .withIncludeTlsSessionInfoHeader(includeTlsSessionInfoHeader)
      .withUriParsingMode(Uri.ParsingMode.Relaxed)
      .withModeledHeaderParsing(false) // Disable most of Akka HTTP's header parsing; use RawHeaders instead

  /**
   * Create Akka HTTP settings for a given port binding.
   *
   * Called by Play when binding a handler to a server port. Will be called once per port. Called by the
   * [[createServerBinding()]] method.
   */
  protected def createServerSettings(
      port: Int,
      connectionContext: ConnectionContext,
      secure: Boolean
  ): ServerSettings = {
    initialSettings
      .withTimeouts(
        initialSettings.timeouts
          .withIdleTimeout(if (secure) httpsIdleTimeout else httpIdleTimeout)
          .withRequestTimeout(requestTimeout)
      )
      // Play needs these headers to fill in fields in its request model
      .withRawRequestUriHeader(true)
      .withRemoteAddressHeader(true)
      .withTransparentHeadRequests(transparentHeadRequests)
      .withServerHeader(serverHeader)
      .withDefaultHostHeader(defaultHostHeader)
      .withParserSettings(parserSettings)
      .withPipeliningLimit(pipeliningLimit)
  }

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
    protected override def reloadValue(tryApp: Try[Application]): ReloadCacheValues = {
      val serverResultUtils      = reloadServerResultUtils(tryApp)
      val forwardedHeaderHandler = reloadForwardedHeaderHandler(tryApp)
      val illegalResponseHeaderValue = ParserSettings.IllegalResponseHeaderValueProcessingMode(
        illegalResponseHeaderValueProcessingMode
      )
      val modelConversion =
        new AkkaModelConversion(serverResultUtils, forwardedHeaderHandler, illegalResponseHeaderValue)
      ReloadCacheValues(
        resultUtils = serverResultUtils,
        modelConversion = modelConversion,
        serverDebugInfo = reloadDebugInfo(tryApp, provider)
      )
    }
  }

  // ----------------------------------------------------------------------
  // CAUTION
  // NO fields (val) below this point that are accessed in handleRequest.
  //    They might not yet be initialized when handleRequest is run for the
  //    first request. In doubt use `lazy val`.
  // ----------------------------------------------------------------------

  /**
   * Bind Akka HTTP to a port to listen for incoming connections. Calls [[createServerSettings()]] to configure the
   * binding and [[handleRequest()]] as a handler for the binding.
   */
  private def createServerBinding(
      port: Int,
      connectionContext: ConnectionContext,
      secure: Boolean
  ): Http.ServerBinding = {
    // TODO: pass in Inet.SocketOption and LoggerAdapter params?
    val bindingFuture: Future[Http.ServerBinding] =
      try {
        Http()(context.actorSystem)
          .bindAndHandleAsync(
            handler = handleRequest(_, connectionContext.isSecure),
            interface = context.config.address,
            port = port,
            connectionContext = connectionContext,
            settings = createServerSettings(port, connectionContext, secure)
          )(context.materializer)
      } catch {
        // Http2SupportNotPresentException is private[akka] so we need to match the name
        case e: Throwable if e.getClass.getSimpleName == "Http2SupportNotPresentException" =>
          throw new RuntimeException(
            "HTTP/2 enabled but akka-http2-support not found. " +
              "Add .enablePlugins(PlayAkkaHttp2Support) in build.sbt",
            e
          )
      }

    Await.result(bindingFuture, bindTimeout)
  }

  // Lazy since it will only be required when HTTPS is bound.
  private lazy val sslContext: SSLContext =
    ServerSSLEngine.createSSLEngineProvider(context.config, applicationProvider).sslContext()

  private val httpServerBinding = context.config.port.map(port =>
    createServerBinding(
      port,
      HttpConnectionContext(),
      secure = false
    )
  )

  private val httpsServerBinding = context.config.sslPort.map { port =>
    val connectionContext =
      try {
        ConnectionContext.httpsServer { () =>
          val engine = sslContext.createSSLEngine()
          engine.setUseClientMode(false)
          // Need has precedence over Want, hence the if/else if
          if (httpsNeedClientAuth) {
            engine.setNeedClientAuth(true)
          } else if (httpsWantClientAuth) {
            engine.setWantClientAuth(true)
          }
          engine
        }
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
    if (httpsNeedClientAuth) {
      Some(TLSClientAuth.need)
    } else if (httpsWantClientAuth) {
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
        case _                    => logger.debug(logMessage)
      }
    }
  }

  /**
   * Get the app's HttpErrorHandler or fallback to a default value
   */
  private def errorHandler(tryApp: Try[Application]): HttpErrorHandler =
    tryApp match {
      case Success(app) => app.errorHandler
      case Failure(_)   => fallbackErrorHandler
    }

  private lazy val fallbackErrorHandler = mode match {
    case Mode.Prod => DefaultHttpErrorHandler
    case _         => DevHttpErrorHandler
  }

  private def resultUtils(tryApp: Try[Application]): ServerResultUtils =
    reloadCache.cachedFrom(tryApp).resultUtils
  private def modelConversion(tryApp: Try[Application]): AkkaModelConversion =
    reloadCache.cachedFrom(tryApp).modelConversion

  private def handleRequest(request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    logger.trace("Http request received by akka-http: " + request)

    import play.core.Execution.Implicits.trampoline

    val decodedRequest = HttpRequestDecoder.decodeRequest(request)
    val tryApp         = applicationProvider.get
    val remoteAddress  = remoteAddressOfRequest(request)

    val convertedRequestHeader: Try[RequestHeader] = modelConversion(tryApp).convertRequestHeader(
      remoteAddress = remoteAddress,
      secureProtocol = secure,
      request = decodedRequest
    )

    // Helper to attach ServerDebugInfo attribute to a RequestHeader
    def attachDebugInfo(rh: RequestHeader): RequestHeader = {
      val debugInfo: Option[ServerDebugInfo] = reloadCache.cachedFrom(tryApp).serverDebugInfo
      ServerDebugInfo.attachToRequestHeader(rh, debugInfo)
    }

    def clientError(statusCode: Int, message: String): (RequestHeader, Handler) = {
      val headers        = modelConversion(tryApp).convertRequestHeadersAkka(decodedRequest)
      val unparsedTarget = Server.createUnparsedRequestTarget(headers.uri)
      val requestHeader =
        modelConversion(tryApp).createRequestHeader(headers, secure, remoteAddress, unparsedTarget, request)
      val debugHeader = attachDebugInfo(requestHeader)
      val result = errorHandler(tryApp).onClientError(
        debugHeader.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("server-backend")),
        statusCode,
        if (message == null) "" else message
      )
      // If there's a problem in parsing the request, then we should close the connection, once done with it
      debugHeader -> Server.actionForResult(result.map(_.withHeaders(HeaderNames.CONNECTION -> "close")))
    }

    val (taggedRequestHeader, handler): (RequestHeader, Handler) = convertedRequestHeader match {
      case Failure(exception) =>
        clientError(Status.BAD_REQUEST, exception.getMessage)
      case Success(untagged) =>
        val debugHeader = attachDebugInfo(untagged)
        Server.getHandlerFor(debugHeader, tryApp, fallbackErrorHandler)
    }

    val responseFuture = executeHandler(
      tryApp,
      decodedRequest,
      taggedRequestHeader,
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
      handler: Handler
  ): Future[HttpResponse] = {
    val upgradeToWebSocket = request.header[UpgradeToWebSocket]

    // default execution context used for executing the action
    implicit val defaultExecutionContext: ExecutionContext = tryApp match {
      case Success(app) => app.actorSystem.dispatcher
      case Failure(_)   => context.actorSystem.dispatcher
    }

    // materializer used for executing the action
    implicit val mat: Materializer = tryApp match {
      case Success(app) => app.materializer
      case Failure(_)   => context.materializer
    }

    val requestBodySource: Either[ByteString, Source[ByteString, Any]] =
      modelConversion(tryApp).convertRequestBody(request)

    (handler, upgradeToWebSocket) match {
      // execute normal action
      case (action: EssentialAction, _) =>
        runAction(tryApp, request, taggedRequestHeader, requestBodySource, action, errorHandler(tryApp), true)
      case (websocket: WebSocket, Some(upgrade)) =>
        websocket(taggedRequestHeader).fast.flatMap {
          case Left(result) =>
            modelConversion(tryApp).convertResult(taggedRequestHeader, result, request.protocol, errorHandler(tryApp))
          case Right(flow) =>
            // For now, like Netty, select an arbitrary subprotocol from the list of subprotocols proposed by the client
            // Eventually it would be better to allow the handler to specify the protocol it selected
            // See also https://github.com/playframework/playframework/issues/7895
            val selectedSubprotocol = upgrade.requestedProtocols.headOption
            Future.successful(
              WebSocketHandler
                .handleWebSocket(upgrade, flow, wsBufferLimit, selectedSubprotocol, wsKeepAliveMode, wsKeepAliveMaxIdle)
            )
        }

      case (websocket: WebSocket, None) =>
        // WebSocket handler for non WebSocket request
        logger.trace(s"Bad websocket request: $request")
        val action = EssentialAction(_ =>
          Accumulator.done(
            Results
              .Status(Status.UPGRADE_REQUIRED)("Upgrade to WebSocket required")
              .withHeaders(
                HeaderNames.UPGRADE    -> "websocket",
                HeaderNames.CONNECTION -> HeaderNames.UPGRADE
              )
          )
        )
        runAction(tryApp, request, taggedRequestHeader, requestBodySource, action, errorHandler(tryApp))
      case (akkaHttpHandler: AkkaHttpHandler, _) =>
        akkaHttpHandler(request)
      case (unhandled, _) => sys.error(s"AkkaHttpServer doesn't handle Handlers of this type: $unhandled")
    }
  }

  private def runAction(
      tryApp: Try[Application],
      request: HttpRequest,
      taggedRequestHeader: RequestHeader,
      requestBodySource: Either[ByteString, Source[ByteString, _]],
      action: EssentialAction,
      errorHandler: HttpErrorHandler,
      deferredBodyParsingAllowed: Boolean = false
  )(implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
    val source = if (request.header[Expect].contains(Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Akka-HTTP, Play signals to continue
      // by requesting demand, Akka-HTTP signals to continue by attaching a sink to the source. See
      // https://github.com/akka/akka/issues/17782 for more details.
      requestBodySource.map(source => Source.lazySource(() => source))
    } else {
      requestBodySource
    }

    // Captures the source and the (implicit) materializer.
    // Meaning no matter if parsing is deferred, it always uses the same materializer.
    def invokeAction(futureAcc: Future[Accumulator[ByteString, Result]], deferBodyParsing: Boolean): Future[Result] =
      // here we use FastFuture so the flatMap shouldn't actually need the executionContext
      futureAcc.fast
        .flatMap { actionAccumulator =>
          {
            if (deferBodyParsing) {
              actionAccumulator.run() // don't parse anything
            } else {
              source match {
                case Left(bytes) if bytes.isEmpty => actionAccumulator.run()
                case Left(bytes)                  => actionAccumulator.run(bytes)
                case Right(s)                     => actionAccumulator.run(s)
              }
            }
          }
        }(mat.executionContext)
        .recoverWith {
          case _: EntityStreamSizeException =>
            errorHandler.onClientError(
              taggedRequestHeader.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("server-backend")),
              Status.REQUEST_ENTITY_TOO_LARGE,
              "Request Entity Too Large"
            )
          case e: Throwable =>
            errorHandler.onServerError(taggedRequestHeader, e)
        }(mat.executionContext)

    val deferBodyParsing = deferredBodyParsingAllowed &&
      Server.routeModifierDefersBodyParsing(serverConfig.underlying.getBoolean("deferBodyParsing"), taggedRequestHeader)
    val futureAcc: Future[Accumulator[ByteString, Result]] = Future(action(if (deferBodyParsing) {
      taggedRequestHeader.addAttr(RequestAttrKey.DeferredBodyParsing, invokeAction _)
    } else {
      taggedRequestHeader
    }))
    val resultFuture: Future[Result] = invokeAction(futureAcc, deferBodyParsing)
    val responseFuture: Future[HttpResponse] = resultFuture.flatMap { result =>
      val cleanedResult: Result = resultUtils(tryApp).prepareCookies(taggedRequestHeader, result)
      modelConversion(tryApp).convertResult(taggedRequestHeader, cleanedResult, request.protocol, errorHandler)
    }
    responseFuture
  }

  mode match {
    case Mode.Test =>
    case _ =>
      httpServerBinding.foreach { http => logger.info(s"Listening for HTTP on ${http.localAddress}") }
      httpsServerBinding.foreach { https => logger.info(s"Listening for HTTPS on ${https.localAddress}") }
  }

  override def stop(): Unit = CoordinatedShutdownSupport.syncShutdown(context.actorSystem, ServerStoppedReason)

  // Using CoordinatedShutdown means that instead of invoking code imperatively in `stop`
  // we have to register it as early as possible as CoordinatedShutdown tasks and
  // then `stop` runs CoordinatedShutdown.
  registerShutdownTasks()
  private def registerShutdownTasks(): Unit = {
    implicit val exCtx: ExecutionContext = context.actorSystem.dispatcher

    // Register all shutdown tasks
    val cs = CoordinatedShutdown(context.actorSystem)
    cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "trace-server-stop-request") { () =>
      if (mode != Mode.Test) logger.info("Stopping Akka HTTP server...")
      Future.successful(Done)
    }

    val serverTerminateTimeout =
      Server.determineServerTerminateTimeout(terminationTimeout, terminationDelay)(context.actorSystem)

    cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "akka-http-server-unbind") { () =>
      def unbind(binding: Option[Http.ServerBinding]): Future[Done] = {
        binding
          .map { binding =>
            logger.info(s"Unbinding ${binding.localAddress}")
            binding.unbind()
          }
          .getOrElse {
            Future.successful(Done)
          }
      }

      for {
        _ <- unbind(httpServerBinding)
        _ <- unbind(httpsServerBinding)
      } yield Done
    }

    cs.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "akka-http-server-terminate") { () =>
      def terminate(binding: Option[Http.ServerBinding]): Future[Done] = {
        binding
          .map { binding =>
            akka.pattern.after(terminationDelay) {
              logger.info(s"Terminating server binding for ${binding.localAddress}")
              binding.terminate(serverTerminateTimeout - 100.millis).map(_ => Done)
            }(context.actorSystem)
          }
          .getOrElse {
            Future.successful(Done)
          }
      }

      for {
        _ <- terminate(httpServerBinding)
        _ <- terminate(httpsServerBinding)
      } yield Done
    }

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "user-provided-server-stop-hook") { () =>
      logger.info("Running provided shutdown stop hooks")
      context.stopHook().map(_ => Done)
    }
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "shutdown-logger") { () =>
      Future {
        super.stop()
        Done
      }
    }
  }

  override lazy val mainAddress: InetSocketAddress = {
    httpServerBinding.orElse(httpsServerBinding).map(_.localAddress).get
  }

  private lazy val Http1Plain = httpServerBinding
    .map(_.localAddress)
    .map(address =>
      ServerEndpoint(
        description = "Akka HTTP HTTP/1.1 (plaintext)",
        scheme = "http",
        host = context.config.address,
        port = address.getPort,
        protocols = Set(PlayHttpProtocol.HTTP_1_0, PlayHttpProtocol.HTTP_1_1),
        serverAttribute = serverHeaderConfig,
        ssl = None
      )
    )

  private lazy val Http1Encrypted = httpsServerBinding
    .map(_.localAddress)
    .map(address =>
      ServerEndpoint(
        description = "Akka HTTP HTTP/1.1 (encrypted)",
        scheme = "https",
        host = context.config.address,
        port = address.getPort,
        protocols = Set(PlayHttpProtocol.HTTP_1_0, PlayHttpProtocol.HTTP_1_1),
        serverAttribute = serverHeaderConfig,
        ssl = Option(sslContext)
      )
    )

  private lazy val Http2Plain = httpServerBinding
    .map(_.localAddress)
    .map(address =>
      ServerEndpoint(
        description = "Akka HTTP HTTP/2 (plaintext)",
        scheme = "http",
        host = context.config.address,
        port = address.getPort,
        protocols = Set(PlayHttpProtocol.HTTP_2_0),
        serverAttribute = serverHeaderConfig,
        ssl = None
      )
    )

  private lazy val Http2Encrypted = httpsServerBinding
    .map(_.localAddress)
    .map(address =>
      ServerEndpoint(
        description = "Akka HTTP HTTP/2 (encrypted)",
        scheme = "https",
        host = context.config.address,
        port = address.getPort,
        protocols = Set(PlayHttpProtocol.HTTP_1_0, PlayHttpProtocol.HTTP_1_1, PlayHttpProtocol.HTTP_2_0),
        serverAttribute = serverHeaderConfig,
        ssl = Option(sslContext)
      )
    )

  override val serverEndpoints: ServerEndpoints = {
    val httpEndpoint  = if (http2Enabled) Http2Plain else Http1Plain
    val httpsEndpoint = if (http2Enabled) Http2Encrypted else Http1Encrypted

    ServerEndpoints(httpEndpoint.toSeq ++ httpsEndpoint.toSeq)
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
      stopHook: () => Future[_]
  )

  object Context {

    /**
     * Create a `Context` object from several common components.
     */
    def fromComponents(
        serverConfig: ServerConfig,
        application: Application,
        stopHook: () => Future[_] = () => Future.successful(())
    ): Context =
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

  protected override def createServerFromRouter(
      serverConf: ServerConfig = ServerConfig()
  )(routes: ServerComponents with BuiltInComponents => Router): Server = {
    new AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents {
      override lazy val serverConfig: ServerConfig = serverConf
      override def router: Router                  = routes(this)
    }.server
  }
}

/**
 * Knows how to create an AkkaHttpServer.
 */
class AkkaHttpServerProvider extends ServerProvider {
  override def createServer(context: ServerProvider.Context): AkkaHttpServer = {
    new AkkaHttpServer(AkkaHttpServer.Context.fromServerProviderContext(context))
  }
}

/**
 * Components for building a simple Akka HTTP Server.
 */
trait AkkaHttpServerComponents extends ServerComponents {
  override lazy val server: AkkaHttpServer = {
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
    extends AkkaHttpServerComponents
    with BuiltInComponents
    with NoHttpFiltersComponents
