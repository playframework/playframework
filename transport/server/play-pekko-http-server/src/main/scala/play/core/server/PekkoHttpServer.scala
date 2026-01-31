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

import com.typesafe.config.Config
import com.typesafe.config.ConfigMemorySize
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.http.play.WebSocketHandler
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers
import org.apache.pekko.http.scaladsl.model.headers.Expect
import org.apache.pekko.http.scaladsl.model.ws.WebSocketUpgrade
import org.apache.pekko.http.scaladsl.model.AttributeKeys
import org.apache.pekko.http.scaladsl.settings.ParserSettings
import org.apache.pekko.http.scaladsl.settings.ServerSettings
import org.apache.pekko.http.scaladsl.util.FastFuture._
import org.apache.pekko.http.scaladsl.ConnectionContext
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.HttpConnectionContext
import org.apache.pekko.http.scaladsl.HttpsConnectionContext
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.TLSClientAuth
import org.apache.pekko.util.ByteString
import org.apache.pekko.Done
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
import play.api.mvc.pekkohttp.PekkoHttpHandler
import play.api.mvc.request.RequestAttrKey
import play.api.routing.Router
import play.core.server.common.ReloadCache
import play.core.server.common.ServerDebugInfo
import play.core.server.common.ServerResultUtils
import play.core.server.pekkohttp.HttpRequestDecoder
import play.core.server.pekkohttp.PekkoModelConversion
import play.core.server.pekkohttp.PekkoServerConfigReader
import play.core.server.ssl.ServerSSLEngine
import play.core.server.Server.ServerStoppedReason
import play.core.ApplicationProvider

/**
 * Starts a Play server using Pekko HTTP.
 */
class PekkoHttpServer(context: PekkoHttpServer.Context) extends Server {

  import PekkoHttpServer._

  assert(
    context.config.port.isDefined || context.config.sslPort.isDefined,
    "PekkoHttpServer must be given at least one of an HTTP and an HTTPS port"
  )

  override def mode: Mode                               = context.config.mode
  override def applicationProvider: ApplicationProvider = context.appProvider

  /** Helper to access server configuration under the `play.server` prefix. */
  private val serverConfig = context.config.configuration.get[Configuration]("play.server")

  /** Helper to access server configuration under the `play.server.pekko` prefix. */
  private val pekkoServerConfig = serverConfig.get[Configuration]("pekko")

  private val pekkoServerConfigReader = new PekkoServerConfigReader(pekkoServerConfig)

  private lazy val initialSettings = ServerSettings(pekkoHttpConfig)

  private val httpIdleTimeout    = serverConfig.get[Duration]("http.idleTimeout")
  private val httpsIdleTimeout   = serverConfig.get[Duration]("https.idleTimeout")
  private val requestTimeout     = pekkoServerConfig.get[Duration]("requestTimeout")
  private val bindTimeout        = pekkoServerConfig.get[FiniteDuration]("bindTimeout")
  private val terminationDelay   = serverConfig.get[FiniteDuration]("waitBeforeTermination")
  private val terminationTimeout =
    serverConfig.getDeprecated[Option[FiniteDuration]]("terminationTimeout", "pekko.terminationTimeout")

  private val maxContentLength =
    Server.getPossiblyInfiniteBytes(serverConfig.underlying, "max-content-length", "pekko.max-content-length")
  private val maxHeaderValueLength =
    serverConfig.getDeprecated[ConfigMemorySize]("max-header-size", "pekko.max-header-value-length").toBytes.toInt
  private val includeTlsSessionInfoHeader = pekkoServerConfig.get[Boolean]("tls-session-info-header")
  private val defaultHostHeader           = pekkoServerConfigReader.getHostHeader.fold(throw _, identity)
  private val transparentHeadRequests     = pekkoServerConfig.get[Boolean]("transparent-head-requests")
  private val serverHeaderConfig          = pekkoServerConfig.getOptional[String]("server-header")
  private val pipeliningLimit             = pekkoServerConfig.get[Int]("pipelining-limit")
  private val serverHeader                = serverHeaderConfig.collect {
    case s if s.nonEmpty => headers.Server(s)
  }

  private val httpsNeedClientAuth                      = serverConfig.get[Boolean]("https.needClientAuth")
  private val httpsWantClientAuth                      = serverConfig.get[Boolean]("https.wantClientAuth")
  private val illegalResponseHeaderValueProcessingMode =
    pekkoServerConfig.get[String]("illegal-response-header-value-processing-mode")
  private val wsBufferLimit      = serverConfig.get[ConfigMemorySize]("websocket.frame.maxLength").toBytes.toInt
  private val wsKeepAliveMode    = serverConfig.get[String]("websocket.periodic-keep-alive-mode")
  private val wsKeepAliveMaxIdle = serverConfig.get[Duration]("websocket.periodic-keep-alive-max-idle")

  private val http2Enabled: Boolean = pekkoServerConfig.getOptional[Boolean]("http2.enabled").getOrElse(false)

  /**
   * Play's configuration for the Pekko HTTP server. Initialized by a call to [[createPekkoHttpConfig()]].
   *
   * Note that the rest of the [[ActorSystem]] outside Pekko HTTP is initialized by the configuration in [[context.config]].
   */
  protected val pekkoHttpConfig: Config = createPekkoHttpConfig()

  /**
   * Creates the configuration used to initialize the Pekko HTTP subsystem. By default this uses the ActorSystem's
   * configuration, with an additional setting patched in to enable or disable HTTP/2.
   */
  protected def createPekkoHttpConfig(): Config =
    Configuration("pekko.http.server.preview.enable-http2" -> http2Enabled)
      .withFallback(Configuration(context.actorSystem.settings.config))
      .underlying

  /** Play's parser settings for Pekko HTTP. Initialized by a call to [[createParserSettings()]]. */
  protected val parserSettings: ParserSettings = createParserSettings()

  /** Called by Play when creating its Pekko HTTP parser settings. Result stored in [[parserSettings]]. */
  protected def createParserSettings(): ParserSettings =
    ParserSettings(pekkoHttpConfig)
      .withMaxContentLength(maxContentLength)
      .withMaxHeaderValueLength(maxHeaderValueLength)
      .withIncludeTlsSessionInfoHeader(includeTlsSessionInfoHeader)
      .withUriParsingMode(Uri.ParsingMode.Relaxed)
      .withModeledHeaderParsing(false) // Disable most of Pekko HTTP's header parsing; use RawHeaders instead

  /**
   * Create Pekko HTTP settings for a given port binding.
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
      .withRemoteAddressAttribute(true)
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
      modelConversion: PekkoModelConversion,
      serverDebugInfo: Option[ServerDebugInfo]
  )

  /**
   * A helper to cache values that are derived from the current application.
   */
  private val reloadCache = new ReloadCache[ReloadCacheValues] {
    protected override def reloadValue(tryApp: Try[Application]): ReloadCacheValues = {
      val serverResultUtils          = reloadServerResultUtils(tryApp)
      val forwardedHeaderHandler     = reloadForwardedHeaderHandler(tryApp)
      val illegalResponseHeaderValue = ParserSettings.IllegalResponseHeaderValueProcessingMode(
        illegalResponseHeaderValueProcessingMode
      )
      val modelConversion =
        new PekkoModelConversion(serverResultUtils, forwardedHeaderHandler, illegalResponseHeaderValue)
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
   * Bind Pekko HTTP to a port to listen for incoming connections. Calls [[createServerSettings()]] to configure the
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
        var serverBuilder = Http()(using context.actorSystem)
          .newServerAt(context.config.address, port)
          .withSettings(createServerSettings(port, connectionContext, secure))
        connectionContext match {
          case httpsContext: HttpsConnectionContext =>
            serverBuilder = serverBuilder.enableHttps(httpsContext)
          case _ =>
        }

        serverBuilder.bind(handleRequest(_, connectionContext.isSecure))
      } catch {
        // Http2SupportNotPresentException is private[pekko] so we need to match the name
        case e: Throwable if e.getClass.getSimpleName == "Http2SupportNotPresentException" =>
          throw new RuntimeException(
            "HTTP/2 enabled but pekko-http2-support not found. " +
              "Add .enablePlugins(PlayPekkoHttp2Support) in build.sbt",
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
        ConnectionContext.httpsServer(() => {
          val engine = sslContext.createSSLEngine()
          createClientAuth() match {
            case Some(auth) if auth == TLSClientAuth.need =>
              engine.setNeedClientAuth(true)
            case Some(auth) if auth == TLSClientAuth.want =>
              engine.setWantClientAuth(true)
            case _ => engine.setUseClientMode(false)
          }
          engine
        })
      } catch {
        case NonFatal(e) =>
          logger.error(s"Cannot load SSL context", e)
          ConnectionContext.noEncryption()
      }
    createServerBinding(port, connectionContext, secure = true)
  }

  /** Creates PekkoHttp TLSClientAuth */
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
    logger.info(s"Enabling HTTP/2 on Pekko HTTP server...")
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
  private def modelConversion(tryApp: Try[Application]): PekkoModelConversion =
    reloadCache.cachedFrom(tryApp).modelConversion

  private def handleRequest(request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    logger.trace("Http request received by pekko-http: " + request)

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
      val headers        = modelConversion(tryApp).convertRequestHeadersPekko(decodedRequest)
      val unparsedTarget = Server.createUnparsedRequestTarget(headers.uri)
      val requestHeader  =
        modelConversion(tryApp).createRequestHeader(headers, secure, remoteAddress, unparsedTarget, request)
      val debugHeader   = attachDebugInfo(requestHeader)
      val maybeEnriched = Server.tryToEnrichHeader(tryApp, debugHeader)
      val result        = errorHandler(tryApp).onClientError(
        maybeEnriched.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("server-backend")),
        statusCode,
        if (message == null) "" else message
      )
      // If there's a problem in parsing the request, then we should close the connection, once done with it
      maybeEnriched -> Server.actionForResult(result.map(_.withHeaders(HeaderNames.CONNECTION -> "close")))
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
    req.attribute(AttributeKeys.remoteAddress) match {
      case Some(attr) =>
        attr.toIP match {
          case Some(address) if address.port.isDefined =>
            new InetSocketAddress(address.ip, address.getPort())
          case _ => throw new IllegalStateException(s"`Remote-Address` attribute address is not valid: $attr")
        }
      case _ => throw new IllegalStateException("`Remote-Address` attribute was missing")
    }
  }

  private def executeHandler(
      tryApp: Try[Application],
      request: HttpRequest,
      taggedRequestHeader: RequestHeader,
      handler: Handler
  ): Future[HttpResponse] = {
    val upgradeToWebSocket = request.attribute(AttributeKeys.webSocketUpgrade)

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
      case (pekkoHttpHandler: PekkoHttpHandler, _) =>
        pekkoHttpHandler(request)
      case (unhandled, _) => sys.error(s"PekkoHttpServer doesn't handle Handlers of this type: $unhandled")
    }
  }

  private def runAction(
      tryApp: Try[Application],
      request: HttpRequest,
      taggedRequestHeader: RequestHeader,
      requestBodySource: Either[ByteString, Source[ByteString, ?]],
      action: EssentialAction,
      errorHandler: HttpErrorHandler,
      deferredBodyParsingAllowed: Boolean = false
  )(implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
    val source = if (request.header[Expect].contains(Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Pekko-HTTP, Play signals to continue
      // by requesting demand, Pekko-HTTP signals to continue by attaching a sink to the source. See
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
        }(using mat.executionContext)
        .recoverWith {
          case _: EntityStreamSizeException =>
            errorHandler.onClientError(
              taggedRequestHeader.addAttr(HttpErrorHandler.Attrs.HttpErrorInfo, HttpErrorInfo("server-backend")),
              Status.REQUEST_ENTITY_TOO_LARGE,
              "Request Entity Too Large"
            )
          case e: Throwable =>
            errorHandler.onServerError(taggedRequestHeader, e)
        }(using mat.executionContext)

    val deferBodyParsing = deferredBodyParsingAllowed &&
      Server.routeModifierDefersBodyParsing(serverConfig.underlying.getBoolean("deferBodyParsing"), taggedRequestHeader)
    val futureAcc: Future[Accumulator[ByteString, Result]] = Future(action(if (deferBodyParsing) {
      taggedRequestHeader.addAttr(RequestAttrKey.DeferredBodyParsing, invokeAction _)
    } else {
      taggedRequestHeader
    }))
    val resultFuture: Future[Result]         = invokeAction(futureAcc, deferBodyParsing)
    val responseFuture: Future[HttpResponse] = resultFuture.flatMap { result =>
      val cleanedResult: Result = resultUtils(tryApp).prepareCookies(taggedRequestHeader, result)
      modelConversion(tryApp).convertResult(taggedRequestHeader, cleanedResult, request.protocol, errorHandler)
    }
    responseFuture
  }

  mode match {
    case Mode.Test =>
    case _         =>
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
      if (mode != Mode.Test) logger.info("Stopping Pekko HTTP server...")
      Future.successful(Done)
    }

    val serverTerminateTimeout =
      Server.determineServerTerminateTimeout(terminationTimeout, terminationDelay)(using context.actorSystem)

    cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "pekko-http-server-unbind") { () =>
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

    cs.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "pekko-http-server-terminate") { () =>
      def terminate(binding: Option[Http.ServerBinding]): Future[Done] = {
        binding
          .map { binding =>
            org.apache.pekko.pattern.after(terminationDelay) {
              logger.info(s"Terminating server binding for ${binding.localAddress}")
              binding.terminate(serverTerminateTimeout - 100.millis).map(_ => Done)
            }(using context.actorSystem)
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
        description = "Pekko HTTP HTTP/1.1 (plaintext)",
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
        description = "Pekko HTTP HTTP/1.1 (encrypted)",
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
        description = "Pekko HTTP HTTP/2 (plaintext)",
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
        description = "Pekko HTTP HTTP/2 (encrypted)",
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
 * Creates an PekkoHttpServer from a given router using [[BuiltInComponents]]:
 *
 * {{{
 *   val server = PekkoHttpServer.fromRouterWithComponents(ServerConfig(port = Some(9002))) { components =>
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
object PekkoHttpServer extends ServerFromRouter {
  private val logger = Logger(classOf[PekkoHttpServer])

  /**
   * The values needed to initialize an [[PekkoHttpServer]].
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
      stopHook: () => Future[?]
  )

  object Context {

    /**
     * Create a `Context` object from several common components.
     */
    def fromComponents(
        serverConfig: ServerConfig,
        application: Application,
        stopHook: () => Future[?] = () => Future.successful(())
    ): Context =
      PekkoHttpServer.Context(
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
      PekkoHttpServer.Context(config, appProvider, actorSystem, materializer, stopHook)
    }
  }

  /**
   * A ServerProvider for creating an PekkoHttpServer.
   */
  implicit val provider: PekkoHttpServerProvider = new PekkoHttpServerProvider

  /**
   * Create a Pekko HTTP server from the given application and server configuration.
   *
   * @param application The application.
   * @param config The server configuration.
   * @return A started Pekko HTTP server, serving the application.
   */
  def fromApplication(application: Application, config: ServerConfig = ServerConfig()): PekkoHttpServer = {
    new PekkoHttpServer(Context.fromComponents(config, application))
  }

  protected override def createServerFromRouter(
      serverConf: ServerConfig = ServerConfig()
  )(routes: ServerComponents & BuiltInComponents => Router): Server = {
    new PekkoHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents {
      override lazy val serverConfig: ServerConfig = serverConf
      override def router: Router                  = routes(this)
    }.server
  }
}

/**
 * Knows how to create an PekkoHttpServer.
 */
class PekkoHttpServerProvider extends ServerProvider {
  override def createServer(context: ServerProvider.Context): PekkoHttpServer = {
    new PekkoHttpServer(PekkoHttpServer.Context.fromServerProviderContext(context))
  }
}

/**
 * Components for building a simple Pekko HTTP Server.
 */
trait PekkoHttpServerComponents extends ServerComponents {
  override lazy val server: PekkoHttpServer = {
    // Start the application first
    Play.start(application)
    new PekkoHttpServer(PekkoHttpServer.Context.fromComponents(serverConfig, application, serverStopHook))
  }

  def application: Application
}

/**
 * A convenient helper trait for constructing an PekkoHttpServer, for example:
 *
 * {{{
 *   val components = new DefaultPekkoHttpServerComponents {
 *     override lazy val router = {
 *       case GET(p"/") => Action(parse.json) { body =>
 *         Ok("Hello")
 *       }
 *     }
 *   }
 *   val server = components.server
 * }}}
 */
trait DefaultPekkoHttpServerComponents
    extends PekkoHttpServerComponents
    with BuiltInComponents
    with NoHttpFiltersComponents
