/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.netty

import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

import akka.stream.Materializer
import com.typesafe.netty.http.DefaultWebSocketHttpResponse
import io.netty.channel._
import io.netty.handler.codec.TooLongFrameException
import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.handler.timeout.IdleStateEvent
import play.api.{ Application, Logger }
import play.api.http.{ DefaultHttpErrorHandler, HeaderNames, HttpErrorHandler, Status }
import play.api.libs.streams.Accumulator
import play.api.mvc.{ EssentialAction, RequestHeader, Results, WebSocket }
import play.core.server.NettyServer
import play.core.server.common.{ ForwardedHeaderHandler, ServerResultUtils }
import play.core.system.RequestIdProvider

import scala.concurrent.Future
import scala.util.{ Failure, Success }

private object PlayRequestHandler {
  private val logger: Logger = Logger(classOf[PlayRequestHandler])
}

private[play] class PlayRequestHandler(val server: NettyServer) extends ChannelInboundHandlerAdapter {

  import PlayRequestHandler._

  // We keep track of whether there are requests in flight.  If there are, we don't respond to read
  // complete, since back pressure is the responsibility of the streams.
  private val requestsInFlight = new AtomicLong()

  // This is used essentially as a queue, each incoming request attaches callbacks to this
  // and replaces it to ensure that responses are written out in the same order that they came
  // in.
  private var lastResponseSent: Future[Unit] = Future.successful(())

  // todo: make forwarded header handling a filter
  private lazy val modelConversion = new NettyModelConversion(
    new ForwardedHeaderHandler(
      ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(server.applicationProvider.get.toOption.map(_.configuration))
    )
  )

  /**
   * Handle the given request.
   */
  def handle(channel: Channel, request: HttpRequest): Future[HttpResponse] = {

    logger.trace("Http request received by netty: " + request)

    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val requestId = RequestIdProvider.requestIDs.incrementAndGet()
    val tryRequest = modelConversion.convertRequest(requestId,
      channel.remoteAddress().asInstanceOf[InetSocketAddress], Option(channel.pipeline().get(classOf[SslHandler])),
      request)

    def clientError(statusCode: Int, message: String) = {
      val requestHeader = modelConversion.createUnparsedRequestHeader(requestId, request,
        channel.remoteAddress().asInstanceOf[InetSocketAddress], Option(channel.pipeline().get(classOf[SslHandler])))
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
            val processor = WebSocketHandler.messageFlowToFrameProcessor(flow, bufferLimit)
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

  //----------------------------------------------------------------
  // Netty overrides

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    logger.trace(s"channelRead: ctx = ${ctx}, msg = ${msg}")
    msg match {
      case req: HttpRequest =>
        requestsInFlight.incrementAndGet()
        // Do essentially the same thing that the mapAsync call in NettyFlowHandler is doing
        val future: Future[HttpResponse] = handle(ctx.channel(), req)

        import play.api.libs.iteratee.Execution.Implicits.trampoline
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
    logger.trace(s"channelReadComplete: ctx = ${ctx}")

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
    request: HttpRequest, app: Option[Application]): Future[HttpResponse] = {
    implicit val mat: Materializer = app.fold(server.materializer)(_.materializer)
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val body = modelConversion.convertRequestBody(request)
    val bodyParser = action(requestHeader)
    val resultFuture = body match {
      case None =>
        bodyParser.run()
      case Some(source) =>
        bodyParser.run(source)
    }

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
  private def errorHandler(app: Option[Application]): HttpErrorHandler =
    app.fold[HttpErrorHandler](DefaultHttpErrorHandler)(_.errorHandler)

  /**
   * Sends a simple response with no body, then closes the connection.
   */
  private def sendSimpleErrorResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus): ChannelFuture = {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    response.headers().set(Names.CONNECTION, "close")
    response.headers().set(Names.CONTENT_LENGTH, "0")
    val f = ctx.channel().write(response)
    f.addListener(ChannelFutureListener.CLOSE)
    f
  }
}
