/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

import akka.stream.Materializer
import com.typesafe.config.ConfigMemorySize
import com.typesafe.netty.http.DefaultWebSocketHttpResponse
import io.netty.channel._
import io.netty.handler.codec.TooLongFrameException
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.timeout.IdleStateEvent
import play.api.http._
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.{ Application, Logger }
import play.core.server.{ NettyServer, Server }
import play.core.server.common.{ ReloadCache, ServerDebugInfo, ServerResultUtils }

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

private object PlayRequestHandler {
  private val logger: Logger = Logger(classOf[PlayRequestHandler])
}

private[play] class PlayRequestHandler(val server: NettyServer, val serverHeader: Option[String]) extends ChannelInboundHandlerAdapter {

  import PlayRequestHandler._

  // We keep track of whether there are requests in flight.  If there are, we don't respond to read
  // complete, since back pressure is the responsibility of the streams.
  private val requestsInFlight = new AtomicLong()

  // This is used essentially as a queue, each incoming request attaches callbacks to this
  // and replaces it to ensure that responses are written out in the same order that they came
  // in.
  private var lastResponseSent: Future[Unit] = Future.successful(())

  /**
   * Values that are cached based on the current application.
   */
  private case class ReloadCacheValues(
      resultUtils: ServerResultUtils,
      modelConversion: NettyModelConversion,
      serverDebugInfo: Option[ServerDebugInfo]
  )

  /**
   * A helper to cache values that are derived from the current application.
   */
  private val reloadCache = new ReloadCache[ReloadCacheValues] {
    override protected def reloadValue(tryApp: Try[Application]): ReloadCacheValues = {
      val serverResultUtils = reloadServerResultUtils(tryApp)
      val forwardedHeaderHandler = reloadForwardedHeaderHandler(tryApp)
      val modelConversion = new NettyModelConversion(serverResultUtils, forwardedHeaderHandler, serverHeader)
      ReloadCacheValues(
        resultUtils = serverResultUtils,
        modelConversion = modelConversion,
        serverDebugInfo = reloadDebugInfo(tryApp, NettyServer.provider)
      )
    }
  }

  private def resultUtils(tryApp: Try[Application]): ServerResultUtils =
    reloadCache.cachedFrom(tryApp).resultUtils
  private def modelConversion(tryApp: Try[Application]): NettyModelConversion =
    reloadCache.cachedFrom(tryApp).modelConversion

  /**
   * Handle the given request.
   */
  def handle(channel: Channel, request: HttpRequest): Future[HttpResponse] = {

    logger.trace("Http request received by netty: " + request)

    import play.core.Execution.Implicits.trampoline

    val tryApp: Try[Application] = server.applicationProvider.get
    val cacheValues: ReloadCacheValues = reloadCache.cachedFrom(tryApp)

    val tryRequest: Try[RequestHeader] = cacheValues.modelConversion.convertRequest(channel, request)

    // Helper to attach ServerDebugInfo attribute to a RequestHeader
    def attachDebugInfo(rh: RequestHeader): RequestHeader = {
      ServerDebugInfo.attachToRequestHeader(rh, cacheValues.serverDebugInfo)
    }

    def clientError(statusCode: Int, message: String): (RequestHeader, Handler) = {
      val unparsedTarget = modelConversion(tryApp).createUnparsedRequestTarget(request)
      val requestHeader = modelConversion(tryApp).createRequestHeader(channel, request, unparsedTarget)
      val debugHeader = attachDebugInfo(requestHeader)
      val result = errorHandler(tryApp).onClientError(debugHeader, statusCode,
        if (message == null) "" else message)
      // If there's a problem in parsing the request, then we should close the connection, once done with it
      debugHeader -> Server.actionForResult(result.map(_.withHeaders(HeaderNames.CONNECTION -> "close")))
    }

    val (requestHeader, handler): (RequestHeader, Handler) = tryRequest match {
      case Failure(exception: TooLongFrameException) => clientError(Status.REQUEST_URI_TOO_LONG, exception.getMessage)
      case Failure(exception) => clientError(Status.BAD_REQUEST, exception.getMessage)
      case Success(untagged) =>
        val debugHeader: RequestHeader = attachDebugInfo(untagged)
        Server.getHandlerFor(debugHeader, tryApp)
    }

    handler match {

      //execute normal action
      case action: EssentialAction =>
        handleAction(action, requestHeader, request, tryApp)

      case ws: WebSocket if requestHeader.headers.get(HeaderNames.UPGRADE).exists(_.equalsIgnoreCase("websocket")) =>
        logger.trace("Serving this request with: " + ws)

        val app = tryApp.get // Guaranteed to be Success for a WebSocket handler
        val wsProtocol = if (requestHeader.secure) "wss" else "ws"
        val wsUrl = s"$wsProtocol://${requestHeader.host}${requestHeader.path}"
        val bufferLimit = app.configuration.getDeprecated[ConfigMemorySize]("play.server.websocket.frame.maxLength", "play.websocket.buffer.limit").toBytes.toInt
        val factory = new WebSocketServerHandshakerFactory(wsUrl, "*", true, bufferLimit)

        val executed = Future(ws(requestHeader))(app.actorSystem.dispatcher)

        import play.core.Execution.Implicits.trampoline
        executed.flatMap(identity).flatMap {
          case Left(result) =>
            // WebSocket was rejected, send result
            val action = EssentialAction(_ => Accumulator.done(result))
            handleAction(action, requestHeader, request, tryApp)
          case Right(flow) =>
            import app.materializer
            val processor = WebSocketHandler.messageFlowToFrameProcessor(flow, bufferLimit)
            Future.successful(new DefaultWebSocketHttpResponse(request.protocolVersion(), HttpResponseStatus.OK,
              processor, factory))

        }.recoverWith {
          case error =>
            app.errorHandler.onServerError(requestHeader, error).flatMap { result =>
              val action = EssentialAction(_ => Accumulator.done(result))
              handleAction(action, requestHeader, request, tryApp)
            }
        }

      //handle bad websocket request
      case ws: WebSocket =>
        logger.trace(s"Bad websocket request: $request")
        val action = EssentialAction(_ => Accumulator.done(
          Results.Status(Status.UPGRADE_REQUIRED)("Upgrade to WebSocket required").withHeaders(
            HeaderNames.UPGRADE -> "websocket",
            HeaderNames.CONNECTION -> HeaderNames.UPGRADE
          )
        ))
        handleAction(action, requestHeader, request, tryApp)

      // This case usually indicates an error in Play's internal routing or handling logic
      case h =>
        val ex = new IllegalStateException(s"Netty server doesn't handle Handlers of this type: $h")
        logger.error(ex.getMessage, ex)
        throw ex
    }
  }

  //----------------------------------------------------------------
  // Netty overrides

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    logger.trace(s"channelRead: ctx = $ctx, msg = $msg")
    msg match {
      case req: HttpRequest =>
        requestsInFlight.incrementAndGet()
        // Do essentially the same thing that the mapAsync call in NettyFlowHandler is doing
        val future: Future[HttpResponse] = handle(ctx.channel(), req)

        import play.core.Execution.Implicits.trampoline
        lastResponseSent = lastResponseSent.flatMap { _ =>
          // Need an explicit cast to Future[Unit] to help scalac out.
          val f: Future[Unit] = future.map { httpResponse =>
            if (requestsInFlight.decrementAndGet() == 0) {
              // Since we've now gone down to zero, we need to issue a
              // read, in case we ignored an earlier read complete
              ctx.read()
            }
            ctx.writeAndFlush(httpResponse)
          }

          f.recover {
            case error: Exception =>
              logger.error("Exception caught in channelRead future", error)
              sendSimpleErrorResponse(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE)
          }
        }
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    logger.trace(s"channelReadComplete: ctx = $ctx")

    // The normal response to read complete is to issue another read,
    // but we only want to do that if there are no requests in flight,
    // this will effectively limit the number of in flight requests that
    // we'll handle by pushing back on the TCP stream, but it also ensures
    // we don't get in the way of the request body reactive streams,
    // which will be using channel read complete and read to implement
    // their own back pressure
    if (requestsInFlight.get() == 0) {
      ctx.read()
    } else {
      // otherwise forward it, so that any handler publishers downstream
      // can handle it
      ctx.fireChannelReadComplete()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause match {
      // IO exceptions happen all the time, it usually just means that the client has closed the connection before fully
      // sending/receiving the response.
      case e: IOException =>
        logger.trace("Benign IO exception caught in Netty", e)
        ctx.channel().close()
      case e: TooLongFrameException =>
        logger.warn("Handling TooLongFrameException", e)
        sendSimpleErrorResponse(ctx, HttpResponseStatus.REQUEST_URI_TOO_LONG)
      case e: IllegalArgumentException if Option(e.getMessage).exists(_.contains("Header value contains a prohibited character")) =>
        // https://github.com/netty/netty/blob/netty-3.9.3.Final/src/main/java/org/jboss/netty/handler/codec/http/HttpHeaders.java#L1075-L1080
        logger.debug("Handling Header value error", e)
        sendSimpleErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST)
      case e =>
        logger.error("Exception caught in Netty", e)
        ctx.channel().close()
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    // AUTO_READ is off, so need to do the first read explicitly.
    // this method is called when the channel is registered with the event loop,
    // so ctx.read is automatically safe here w/o needing an isRegistered().
    ctx.read()
  }

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case idle: IdleStateEvent if ctx.channel().isOpen =>
        logger.trace(s"Closing connection due to idle timeout")
        ctx.close()
      case _ => super.userEventTriggered(ctx, evt)
    }
  }

  //----------------------------------------------------------------
  // Private methods

  /**
   * Handle an essential action.
   */
  private def handleAction(action: EssentialAction, requestHeader: RequestHeader,
    request: HttpRequest, tryApp: Try[Application]): Future[HttpResponse] = {
    implicit val mat: Materializer = tryApp match {
      case Success(app) => app.materializer
      case Failure(_) => server.materializer
    }
    import play.core.Execution.Implicits.trampoline

    // Execute the action on the Play default execution context
    val actionFuture = Future(action(requestHeader))(mat.executionContext)
    for {
      // Execute the action and get a result, calling errorHandler if errors happen in this process
      actionResult <- actionFuture.flatMap { acc =>
        val body = modelConversion(tryApp).convertRequestBody(request)
        body match {
          case None => acc.run()
          case Some(source) => acc.run(source)
        }
      }.recoverWith {
        case error =>
          logger.error("Cannot invoke the action", error)
          errorHandler(tryApp).onServerError(requestHeader, error)
      }
      // Clean and validate the action's result
      validatedResult <- {
        val cleanedResult = resultUtils(tryApp).prepareCookies(requestHeader, actionResult)
        resultUtils(tryApp).validateResult(requestHeader, cleanedResult, errorHandler(tryApp))
      }
      // Convert the result to a Netty HttpResponse
      convertedResult <- modelConversion(tryApp)
        .convertResult(validatedResult, requestHeader, request.protocolVersion(), errorHandler(tryApp))
    } yield convertedResult
  }

  /**
   * Get the error handler for the application.
   */
  private def errorHandler(tryApp: Try[Application]): HttpErrorHandler =
    tryApp match {
      case Success(app) => app.errorHandler
      case Failure(_) => DefaultHttpErrorHandler
    }

  /**
   * Sends a simple response with no body, then closes the connection.
   */
  private def sendSimpleErrorResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus): ChannelFuture = {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    response.headers().set(HttpHeaderNames.CONNECTION, "close")
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0")
    val f = ctx.channel().write(response)
    f.addListener(ChannelFutureListener.CLOSE)
    f
  }
}
