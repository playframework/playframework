package play.core.server.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Expect
import akka.stream.Materializer
import akka.stream.scaladsl._
import java.net.InetSocketAddress
import akka.util.ByteString
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.core.ApplicationProvider
import play.core.server._
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }
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
    stopHook: () => Future[Unit]) extends Server {

  import AkkaHttpServer._

  assert(config.port.isDefined, "AkkaHttpServer must be given an HTTP port")
  assert(!config.sslPort.isDefined, "AkkaHttpServer cannot handle HTTPS")

  def mode = config.mode

  // Remember that some user config may not be available in development mode due to
  // its unusual ClassLoader.
  implicit val system = actorSystem
  implicit val mat = materializer

  val address: InetSocketAddress = {
    // Listen for incoming connections and handle them with the `handleRequest` method.

    // TODO: pass in Inet.SocketOption, ServerSettings and LoggerAdapter params?
    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = config.address, port = config.port.get)

    val connectionSink: Sink[Http.IncomingConnection, _] = Sink.foreach { connection: Http.IncomingConnection =>
      connection.handleWithAsyncHandler(handleRequest(connection.remoteAddress, _))
    }

    val bindingFuture: Future[Http.ServerBinding] = serverSource.to(connectionSink).run()

    val bindTimeout = PlayConfig(config.configuration).get[Duration]("play.akka.http-bind-timeout")
    Await.result(bindingFuture, bindTimeout).localAddress
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

  private def handleRequest(remoteAddress: InetSocketAddress, request: HttpRequest): Future[HttpResponse] = {
    val requestId = requestIDs.incrementAndGet()
    val (convertedRequestHeader, requestBodySource) = modelConversion.convertRequest(
      requestId = requestId,
      remoteAddress = remoteAddress,
      secureProtocol = false, // TODO: Change value once HTTPS connections are supported
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
    requestBodySource: Source[ByteString, _],
    handler: Handler): Future[HttpResponse] = handler match {
    //execute normal action
    case action: EssentialAction =>
      val actionWithErrorHandling = EssentialAction { rh =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        action(rh).recoverWith {
          case error => handleHandlerError(tryApp, taggedRequestHeader, error)
        }
      }
      executeAction(tryApp, request, taggedRequestHeader, requestBodySource, actionWithErrorHandling)
    case unhandled => sys.error(s"AkkaHttpServer doesn't handle Handlers of this type: $unhandled")
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
    requestBodySource: Source[ByteString, _],
    action: EssentialAction): Future[HttpResponse] = {

    import play.api.libs.iteratee.Execution.Implicits.trampoline
    val actionAccumulator: Accumulator[ByteString, Result] = action(taggedRequestHeader)

    val source = if (request.header[Expect].exists(_ == Expect.`100-continue`)) {
      // If we expect 100 continue, then we must not feed the source into the accumulator until the accumulator
      // requests demand.  This is due to a semantic mismatch between Play and Akka-HTTP, Play signals to continue
      // by requesting demand, Akka-HTTP signals to continue by attaching a sink to the source. See
      // https://github.com/akka/akka/issues/17782 for more details.
      Source(new MaterialiseOnDemandPublisher(requestBodySource))
    } else {
      requestBodySource
    }

    val resultFuture: Future[Result] = actionAccumulator.run(source)
    val responseFuture: Future[HttpResponse] = resultFuture.flatMap { result =>
      val cleanedResult: Result = ServerResultUtils.cleanFlashCookie(taggedRequestHeader, result)
      modelConversion.convertResult(taggedRequestHeader, cleanedResult, request.protocol)
    }
    responseFuture
  }

  // TODO: Log information about the address we're listening on, like in NettyServer
  mode match {
    case Mode.Test =>
    case _ =>
  }

  override def stop() {

    applicationProvider.current.foreach(Play.stop)

    try {
      super.stop()
    } catch {
      case NonFatal(e) => logger.error("Error while stopping logger", e)
    }

    mode match {
      case Mode.Test =>
      case _ => logger.info("Stopping server...")
    }

    // TODO: Orderly shutdown
    system.shutdown()

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    Await.result(stopHook(), Duration.Inf)
  }

  override lazy val mainAddress = {
    // TODO: Handle HTTPS here, like in NettyServer
    address
  }

  def httpPort = Some(address.getPort)

  def httpsPort = None
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
