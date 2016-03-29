/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import java.security.{ Provider, SecureRandom }
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.play.WebSocketHandler
import akka.http.scaladsl.{ ConnectionContext, Http }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Expect
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.stream.Materializer
import akka.stream.scaladsl._
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.settings.ServerSettings
import akka.util.ByteString
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.streams.{ Accumulator, MaterializeOnDemandPublisher }
import play.api.mvc._
import play.core.ApplicationProvider
import play.core.server._
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

  def mode = config.mode

  // Remember that some user config may not be available in development mode due to
  // its unusual ClassLoader.
  implicit val system = actorSystem
  implicit val mat = materializer

  private def createServerBinding(port: Int, connectionContext: ConnectionContext, secure: Boolean): Http.ServerBinding = {
    // Listen for incoming connections and handle them with the `handleRequest` method.

    val initialSettings = ServerSettings(system)
    val idleTimeout: Duration = (if (secure) {
      config.configuration.getMilliseconds("play.server.https.idleTimeout")
    } else {
      config.configuration.getMilliseconds("play.server.http.idleTimeout")
    }).map { timeoutMS =>
      logger.trace(s"using idle timeout of $timeoutMS` ms on port $port")
      Duration.apply(timeoutMS, TimeUnit.MILLISECONDS)
    }.getOrElse(initialSettings.timeouts.idleTimeout)
    val serverSettings = initialSettings.withTimeouts(initialSettings.timeouts.withIdleTimeout(idleTimeout))

    // TODO: pass in Inet.SocketOption and LoggerAdapter params?
    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = config.address, port = port, connectionContext = connectionContext, settings = serverSettings)

    val connectionSink: Sink[Http.IncomingConnection, _] = Sink.foreach { connection: Http.IncomingConnection =>
      connection.handleWithAsyncHandler(handleRequest(connection.remoteAddress, _, connectionContext.isSecure))
    }

    val bindingFuture: Future[Http.ServerBinding] = serverSource.to(connectionSink).run()

    val bindTimeout = PlayConfig(config.configuration).get[Duration]("play.akka.http-bind-timeout")
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
  private lazy val modelConversion: ModelConversion = {
    val forwardedHeaderHandler = new ForwardedHeaderHandler(
      ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(applicationProvider.get.toOption.map(_.configuration)))
    new ModelConversion(forwardedHeaderHandler)
  }

  private def handleRequest(remoteAddress: InetSocketAddress, request: HttpRequest, secure: Boolean): Future[HttpResponse] = {
    val requestId = requestIDs.incrementAndGet()
    val (convertedRequestHeader, requestBodySource) = modelConversion.convertRequest(
      requestId = requestId,
      remoteAddress = remoteAddress,
      secureProtocol = secure,
      request = request)
    val (taggedRequestHeader, handler, newTryApp) = getHandler(convertedRequestHeader)
    val responseFuture = executeHandler(
      newTryApp,
      request,
      taggedRequestHeader,
      requestBodySource,
      handler
    )
    responseFuture
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
    requestBodySource: Option[Source[ByteString, _]],
    handler: Handler): Future[HttpResponse] = {

    val upgradeToWebSocket = request.header[UpgradeToWebSocket]

    (handler, upgradeToWebSocket) match {
      //execute normal action
      case (action: EssentialAction, _) =>
        val actionWithErrorHandling = EssentialAction { rh =>
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          action(rh).recoverWith {
            case error => handleHandlerError(tryApp, taggedRequestHeader, error)
          }
        }
        executeAction(tryApp, request, taggedRequestHeader, requestBodySource, actionWithErrorHandling)

      case (websocket: WebSocket, Some(upgrade)) =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline

        websocket(taggedRequestHeader).map {
          case Left(result) =>
            modelConversion.convertResult(taggedRequestHeader, result, request.protocol)
          case Right(flow) =>
            WebSocketHandler.handleWebSocket(upgrade, flow, 16384)
        }

      case (websocket: WebSocket, None) =>
        // WebSocket handler for non WebSocket request
        sys.error(s"WebSocket returned for non WebSocket request")
      case (unhandled, _) => sys.error(s"AkkaHttpServer doesn't handle Handlers of this type: $unhandled")

    }
  }

  /** Error handling to use during execution of a handler (e.g. an action) */
  private def handleHandlerError(tryApp: Try[Application], rh: RequestHeader, t: Throwable): Future[Result] = {
    tryApp match {
      case Success(app) => app.errorHandler.onServerError(rh, t)
      case Failure(_) => DefaultHttpErrorHandler.onServerError(rh, t)
    }
  }

  def executeAction(
    tryApp: Try[Application],
    request: HttpRequest,
    taggedRequestHeader: RequestHeader,
    requestBodySource: Option[Source[ByteString, _]],
    action: EssentialAction): Future[HttpResponse] = {

    import play.api.libs.iteratee.Execution.Implicits.trampoline
    val actionAccumulator: Accumulator[ByteString, Result] = action(taggedRequestHeader)

    val source = if (request.header[Expect].contains(Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Akka-HTTP, Play signals to continue
      // by requesting demand, Akka-HTTP signals to continue by attaching a sink to the source. See
      // https://github.com/akka/akka/issues/17782 for more details.
      requestBodySource.map(source => Source.fromPublisher(new MaterializeOnDemandPublisher(source)))
        .orElse(Some(Source.empty))
    } else {
      requestBodySource
    }

    val resultFuture: Future[Result] = source match {
      case None => actionAccumulator.run()
      case Some(s) => actionAccumulator.run(s)
    }
    val responseFuture: Future[HttpResponse] = resultFuture.map { result =>
      val cleanedResult: Result = ServerResultUtils.cleanFlashCookie(taggedRequestHeader, result)
      modelConversion.convertResult(taggedRequestHeader, cleanedResult, request.protocol)
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

  override lazy val mainAddress = {
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
  implicit val provider = new AkkaHttpServerProvider

}

/**
 * Knows how to create an AkkaHttpServer.
 */
class AkkaHttpServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context) =
    new AkkaHttpServer(context.config, context.appProvider, context.actorSystem, context.materializer,
      context.stopHook)
}
