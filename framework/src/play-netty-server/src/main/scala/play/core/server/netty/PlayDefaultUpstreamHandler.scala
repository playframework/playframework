/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.websocketx.{ WebSocketFrame, TextWebSocketFrame, BinaryWebSocketFrame }
import org.jboss.netty.handler.codec.frame.TooLongFrameException
import org.jboss.netty.handler.ssl._

import org.jboss.netty.channel.group._
import play.api._
import play.api.http.{ HttpErrorHandler, DefaultHttpErrorHandler }
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.core.server.Server
import play.core.server.common.{ ForwardedHeaderHandler, ServerRequestUtils, ServerResultUtils }
import play.core.system.RequestIdProvider
import play.core.websocket._
import scala.collection.JavaConverters._
import scala.util.control.Exception
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import scala.concurrent.Future
import java.net.{ InetSocketAddress, URI }
import java.io.IOException
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame

private[play] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup) extends SimpleChannelUpstreamHandler with WebSocketHandler with RequestBodyHandler {

  import PlayDefaultUpstreamHandler._

  private lazy val forwardedHeaderHandler = new ForwardedHeaderHandler(
    ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(server.applicationProvider.get.toOption.map(_.configuration)))

  /**
   * Sends a simple response with no body, then closes the connection.
   */
  private def sendSimpleErrorResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus): Unit = {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    response.headers().set(Names.CONNECTION, "close")
    response.headers().set(Names.CONTENT_LENGTH, "0")
    ctx.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, event: ExceptionEvent): Unit = {

    event.getCause match {
      // IO exceptions happen all the time, it usually just means that the client has closed the connection before fully
      // sending/receiving the response.
      case e: IOException =>
        logger.trace("Benign IO exception caught in Netty", e)
        event.getChannel.close()
      case e: TooLongFrameException =>
        logger.warn("Handling TooLongFrameException", e)
        sendSimpleErrorResponse(ctx, HttpResponseStatus.REQUEST_URI_TOO_LONG)
      case e: IllegalArgumentException if Option(e.getMessage).exists(_.contains("Header value contains a prohibited character")) =>
        // https://github.com/netty/netty/blob/netty-3.9.3.Final/src/main/java/org/jboss/netty/handler/codec/http/HttpHeaders.java#L1075-L1080
        logger.debug("Handling Header value error", e)
        sendSimpleErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST)
      case e =>
        logger.error("Exception caught in Netty", e)
        event.getChannel.close()
    }

  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    Option(ctx.getPipeline.get(classOf[SslHandler])).map { sslHandler =>
      sslHandler.handshake()
    }
  }

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    allChannels.add(e.getChannel)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {

      case nettyHttpRequest: HttpRequest =>

        logger.trace("Http request received by netty: " + nettyHttpRequest)
        val websocketableRequest = websocketable(nettyHttpRequest)
        var nettyVersion = nettyHttpRequest.getProtocolVersion
        val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
        val rHeaders: Headers = getHeaders(nettyHttpRequest)

        def rRemoteAddress = ServerRequestUtils.findRemoteAddress(
          forwardedHeaderHandler,
          rHeaders,
          connectionRemoteAddress = e.getRemoteAddress.asInstanceOf[InetSocketAddress])
        def rSecure = ServerRequestUtils.findSecureProtocol(
          forwardedHeaderHandler,
          rHeaders,
          connectionSecureProtocol = ctx.getPipeline.get(classOf[SslHandler]) != null
        )

        def tryToCreateRequest = {
          val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)
          // wrapping into URI to handle absoluteURI
          val path = new URI(nettyUri.getPath).getRawPath
          createRequestHeader(path, parameters)
        }

        def createRequestHeader(parsedPath: String, parameters: Map[String, Seq[String]] = Map.empty[String, Seq[String]]) = {
          //mapping netty request to Play's
          val untaggedRequestHeader = new RequestHeader {
            val id = RequestIdProvider.requestIDs.incrementAndGet
            val tags = Map.empty[String, String]
            def uri = nettyHttpRequest.getUri
            def path = parsedPath
            def method = nettyHttpRequest.getMethod.getName
            def version = nettyVersion.getText
            def queryString = parameters
            def headers = rHeaders
            lazy val remoteAddress = rRemoteAddress
            lazy val secure = rSecure
            def username = None
          }
          untaggedRequestHeader
        }

        val (requestHeader, handler: Either[Future[Result], (Handler, Application)] @unchecked) = Exception
          .allCatch[RequestHeader].either(tryToCreateRequest).fold(
            e => {
              // use unparsed path
              val rh = createRequestHeader(nettyUri.getPath)
              val result = Future
                .successful(()) // Create a dummy future
                .flatMap { _ =>
                  // Call errorHandler in another context, don't block here
                  errorHandler(server.applicationProvider.get.toOption).onClientError(rh, 400, e.getMessage)
                }(play.api.libs.iteratee.Execution.trampoline)
              (rh, Left(result))
            },
            rh => server.getHandlerFor(rh) match {
              case directResult @ Left(_) => (rh, directResult)
              case Right((taggedRequestHeader, handler, application)) => (taggedRequestHeader, Right((handler, application)))
            }
          )

        // It is a pre-requesite that we're using the http pipelining capabilities provided and that we have a
        // handler downstream from this one that produces these events.
        implicit val msgCtx = ctx
        implicit val oue = e.asInstanceOf[OrderedUpstreamMessageEvent]

        handler match {
          //execute normal action
          case Right((action: EssentialAction, app)) =>
            val a = EssentialAction { rh =>
              import play.api.libs.iteratee.Execution.Implicits.trampoline
              Iteratee.flatten(action(rh).unflatten.map(_.it).recover {
                case error =>
                  Iteratee.flatten(
                    app.errorHandler.onServerError(requestHeader, error).map(result => Done(result, Input.Empty))
                  ): Iteratee[Array[Byte], Result]
              })
            }
            handleAction(a, Some(app))

          case Right((ws @ WebSocket(f), app)) if websocketableRequest.check =>
            logger.trace("Serving this request with: " + ws)

            val executed = Future(f(requestHeader))(play.api.libs.concurrent.Execution.defaultContext)

            import play.api.libs.iteratee.Execution.Implicits.trampoline
            executed.flatMap(identity).map {
              case Left(result) =>
                // WebSocket was rejected, send result
                val a = EssentialAction(_ => Done(result, Input.Empty))
                handleAction(a, Some(app))
              case Right(socket) =>
                val bufferLimit = app.configuration.getBytes("play.websocket.buffer.limit").getOrElse(65536L)

                val enumerator = websocketHandshake(ctx, nettyHttpRequest, e, bufferLimit)(ws.inFormatter)
                socket(enumerator, socketOut(ctx)(ws.outFormatter))
            }.recover {
              case error =>
                app.errorHandler.onServerError(requestHeader, error).map { result =>
                  val a = EssentialAction(_ => Done(result, Input.Empty))
                  handleAction(a, Some(app))
                }
            }

          //handle bad websocket request
          case Right((WebSocket(_), app)) =>
            logger.trace("Bad websocket request")
            val a = EssentialAction(_ => Done(Results.BadRequest, Input.Empty))
            handleAction(a, Some(app))

          case Left(e) =>
            logger.trace("No handler, got direct result: " + e)
            import play.api.libs.iteratee.Execution.Implicits.trampoline
            val a = EssentialAction(_ => Iteratee.flatten(e.map(result => Done(result, Input.Empty))))
            handleAction(a, None)

        }

        def handleAction(action: EssentialAction, app: Option[Application]) {
          logger.trace("Serving this request with: " + action)

          val bodyParser = Iteratee.flatten(
            scala.concurrent.Future(action(requestHeader))(play.api.libs.concurrent.Execution.defaultContext)
          )

          import play.api.libs.iteratee.Execution.Implicits.trampoline

          val expectContinue: Option[_] = requestHeader.headers.get("Expect").filter(_.equalsIgnoreCase("100-continue"))

          // Regardless of whether the client is expecting 100 continue or not, we need to feed the body here in the
          // Netty thread, so that the handler is replaced in this thread, so that if the client does start sending
          // body chunks (which it might according to the HTTP spec if we're slow to respond), we can handle them.

          val eventuallyResult: Future[Result] = if (nettyHttpRequest.isChunked) {

            val pipeline = ctx.getChannel.getPipeline
            val result = newRequestBodyUpstreamHandler(bodyParser, { handler =>
              pipeline.replace("handler", "handler", handler)
            }, {
              pipeline.replace("handler", "handler", this)
            })

            result

          } else {

            val bodyEnumerator = {
              val body = {
                val cBuffer = nettyHttpRequest.getContent
                val bytes = new Array[Byte](cBuffer.readableBytes())
                cBuffer.readBytes(bytes)
                bytes
              }
              Enumerator(body).andThen(Enumerator.enumInput(EOF))
            }

            bodyEnumerator |>>> bodyParser
          }

          // An iteratee containing the result and the sequence number.
          // Sequence number will be 1 if a 100 continue response has been sent, otherwise 0.
          val eventuallyResultWithSequence: Future[(Result, Int)] = expectContinue match {
            case Some(_) => {
              bodyParser.unflatten.flatMap {
                case Step.Cont(k) =>
                  sendDownstream(0, false, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
                  eventuallyResult.map((_, 1))
                case Step.Done(result, _) => {
                  // Return the result immediately, and ensure that the connection is set to close
                  // Connection must be set to close because whatever comes next in the stream is either the request
                  // body, because the client waited too long for our response, or the next request, and there's no way
                  // for us to know which.  See RFC2616 Section 8.2.3.
                  Future.successful((result.copy(connection = HttpConnection.Close), 0))
                }
                case Step.Error(msg, _) => {
                  e.getChannel.setReadable(true)
                  val error = new RuntimeException("Body parser iteratee in error: " + msg)
                  val result = errorHandler(app).onServerError(requestHeader, error)
                  result.map(r => (r.copy(connection = HttpConnection.Close), 0))
                }
              }
            }
            case None => eventuallyResult.map((_, 0))
          }

          val sent = eventuallyResultWithSequence.recoverWith {
            case error =>
              logger.error("Cannot invoke the action, eventually got an error: " + error)
              e.getChannel.setReadable(true)
              errorHandler(app).onServerError(requestHeader, error)
                .map((_, 0))
          }.flatMap {
            case (result, sequence) =>
              val cleanedResult = ServerResultUtils.cleanFlashCookie(requestHeader, result)
              NettyResultStreamer.sendResult(requestHeader, cleanedResult, nettyVersion, sequence)
          }

        }

      case unexpected => logger.error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

    }
  }

  private def errorHandler(app: Option[Application]) = app.fold[HttpErrorHandler](DefaultHttpErrorHandler)(_.errorHandler)

  def socketOut[A](ctx: ChannelHandlerContext)(frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): Iteratee[A, Unit] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    val channel = ctx.getChannel
    val basicFrameFormatter = frameFormatter.asInstanceOf[BasicFrameFormatter[A]]

    import NettyFuture._

    def iteratee: Iteratee[A, _] = Cont {
      case El(e) =>
        val basicFrame: BasicFrame = basicFrameFormatter.toFrame(e)
        val nettyFrame: WebSocketFrame = basicFrame match {
          case TextFrame(text) => new TextWebSocketFrame(true, 0, text)
          case BinaryFrame(bytes) => new BinaryWebSocketFrame(true, 0, ChannelBuffers.wrappedBuffer(bytes))
        }
        Iteratee.flatten(channel.write(nettyFrame).toScala.map(_ => iteratee))
      case e @ EOF =>
        if (channel.isOpen) {
          Iteratee.flatten(for {
            _ <- channel.write(new CloseWebSocketFrame(WebSocketNormalClose, "")).toScala
            _ <- channel.close().toScala
          } yield Done((), e))
        } else Done((), e)
      case Empty => iteratee
    }

    iteratee.map(_ => ())
  }

  def getHeaders(nettyRequest: HttpRequest): Headers = {
    val pairs = nettyRequest.headers().entries().asScala.map(h => h.getKey -> h.getValue)
    new Headers(pairs)
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)(implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamChannelEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }
}

object PlayDefaultUpstreamHandler {
  private val logger = Logger(classOf[PlayDefaultUpstreamHandler])
}
