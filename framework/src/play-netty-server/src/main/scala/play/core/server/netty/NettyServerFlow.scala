package play.core.server.netty

import java.net.InetSocketAddress

import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.typesafe.netty.http.DefaultWebSocketHttpResponse
import io.netty.channel.Channel
import io.netty.handler.codec.TooLongFrameException
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.ssl.SslHandler
import play.api.Application
import play.api.Logger
import play.api.http._
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.core.server.NettyServer
import play.core.server.common.{ ServerResultUtils, ForwardedHeaderHandler }
import play.core.system.RequestIdProvider

import scala.concurrent.Future
import scala.util.{ Success, Failure }

private[server] class NettyServerFlow(server: NettyServer) {

  private val logger = Logger(classOf[NettyServerFlow])
  // todo: make forwarded header handling a filter
  private lazy val modelConversion = new NettyModelConversion(
    new ForwardedHeaderHandler(
      ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(server.applicationProvider.get.toOption.map(_.configuration))
    )
  )

  /**
   * Create a flow to handle the given channel.
   */
  def createFlow(channel: Channel): Flow[HttpRequest, HttpResponse, _] = {
    Flow[HttpRequest].mapAsync(2) { request =>
      handle(channel, request)
    }
  }

  /**
   * Handle the given request.
   */
  private def handle(channel: Channel, request: HttpRequest): Future[HttpResponse] = {

    logger.trace("Http request received by netty: " + request)

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val requestId = RequestIdProvider.requestIDs.incrementAndGet()
    val tryRequest = modelConversion.convertRequest(requestId,
      channel.remoteAddress().asInstanceOf[InetSocketAddress], channel.pipeline().get(classOf[SslHandler]) != null,
      request)

    def clientError(statusCode: Int, message: String) = {
      val requestHeader = modelConversion.createUnparsedRequestHeader(requestId, request,
        channel.remoteAddress().asInstanceOf[InetSocketAddress], channel.pipeline().get(classOf[SslHandler]) != null)
      val result = errorHandler(server.applicationProvider.current).onClientError(requestHeader, statusCode,
        if (message == null) "" else message)
      // If there's a problem in parsing the request, then we should close the connection, once done with it
      requestHeader -> Left(result.map(_.withHeaders(HeaderNames.CONNECTION -> "close")))
    }

    val (requestHeader, resultOrHandler) = tryRequest match {

      case Failure(exception: TooLongFrameException) => clientError(Status.REQUEST_URI_TOO_LONG, exception.getMessage)
      case Failure(exception) => clientError(Status.BAD_REQUEST, exception.getMessage)
      case Success(untagged) =>
        server.getHandlerFor(untagged) match {

          case Left(directResult) =>
            untagged -> Left(directResult)

          case Right((taggedRequestHeader, handler, application)) =>
            taggedRequestHeader -> Right((handler, application))
        }

    }

    resultOrHandler match {

      //execute normal action
      case Right((action: EssentialAction, app)) =>
        val recovered = EssentialAction { rh =>
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          action(rh).recoverWith {
            case error => app.errorHandler.onServerError(rh, error)
          }
        }
        handleAction(recovered, requestHeader, request, Some(app))

      case Right((ws: WebSocket, app)) if requestHeader.headers.get(HeaderNames.UPGRADE).exists(_.equalsIgnoreCase("websocket")) =>
        logger.trace("Serving this request with: " + ws)

        val wsProtocol = if (requestHeader.secure) "wss" else "ws"
        val wsUrl = s"$wsProtocol://${requestHeader.host}${requestHeader.path}"
        val bufferLimit = app.configuration.getBytes("play.websocket.buffer.limit").getOrElse(65536L).asInstanceOf[Int]
        val factory = new WebSocketServerHandshakerFactory(wsUrl, "*", true, bufferLimit)

        val executed = Future(ws(requestHeader))(play.api.libs.concurrent.Execution.defaultContext)

        import play.api.libs.iteratee.Execution.Implicits.trampoline
        executed.flatMap(identity).flatMap {
          case Left(result) =>
            // WebSocket was rejected, send result
            val action = EssentialAction(_ => Accumulator.done(result))
            handleAction(action, requestHeader, request, Some(app))
          case Right(flow) =>
            import app.materializer
            val processor = WebSocketHandler.messageFlowToFrameFlow(flow, bufferLimit)
              .toProcessor.run()
            Future.successful(new DefaultWebSocketHttpResponse(request.getProtocolVersion, HttpResponseStatus.OK,
              processor, factory))

        }.recoverWith {
          case error =>
            app.errorHandler.onServerError(requestHeader, error).flatMap { result =>
              val action = EssentialAction(_ => Accumulator.done(result))
              handleAction(action, requestHeader, request, Some(app))
            }
        }

      //handle bad websocket request
      case Right((ws: WebSocket, app)) =>
        logger.trace("Bad websocket request")
        val action = EssentialAction(_ => Accumulator.done(
          Results.Status(Status.UPGRADE_REQUIRED)("Upgrade to WebSocket required").withHeaders(
            HeaderNames.UPGRADE -> "websocket",
            HeaderNames.CONNECTION -> HeaderNames.UPGRADE
          )
        ))
        handleAction(action, requestHeader, request, Some(app))

      case Left(e) =>
        logger.trace("No handler, got direct result: " + e)
        val action = EssentialAction(_ => Accumulator.done(e))
        handleAction(action, requestHeader, request, None)

    }
  }

  /**
   * Handle an essential action.
   */
  private def handleAction(action: EssentialAction, requestHeader: RequestHeader,
    request: HttpRequest, app: Option[Application]): Future[HttpResponse] = {

    implicit val mat: Materializer = app.fold(server.materializer)(_.materializer)
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val body = modelConversion.convertRequestBody(request)
    val bodyParser = action(requestHeader)
    val resultFuture = bodyParser.run(body)

    resultFuture.recoverWith {
      case error =>
        logger.error("Cannot invoke the action", error)
        errorHandler(app).onServerError(requestHeader, error)
    }.map {
      case result =>
        val cleanedResult = ServerResultUtils.cleanFlashCookie(requestHeader, result)
        val validated = ServerResultUtils.validateResult(requestHeader, cleanedResult)
        modelConversion.convertResult(validated, requestHeader, request.getProtocolVersion)
    }
  }

  /**
   * Get the error handler for the application.
   */
  private def errorHandler(app: Option[Application]) =
    app.fold[HttpErrorHandler](DefaultHttpErrorHandler)(_.errorHandler)

}
