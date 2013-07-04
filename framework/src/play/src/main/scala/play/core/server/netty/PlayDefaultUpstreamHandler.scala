package play.core.server.netty

import scala.language.reflectiveCalls

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.ssl._

import org.jboss.netty.channel.group._
import play.core._
import server.Server
import play.api._
import play.api.mvc._
import play.api.http.HeaderNames.X_FORWARDED_FOR
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import scala.collection.JavaConverters._
import scala.util.control.Exception
import com.typesafe.netty.http.pipelining.{OrderedDownstreamMessageEvent, OrderedUpstreamMessageEvent}
import scala.concurrent.Future


private[server] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup) extends SimpleChannelUpstreamHandler with WebSocketHandler with RequestBodyHandler {

  implicit val internalExecutionContext =  play.core.Execution.internalContext

  private val requestIDs = new java.util.concurrent.atomic.AtomicLong(0)

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    Logger.trace("Exception caught in Netty", e.getCause)
    e.getChannel.close()
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    Option(ctx.getPipeline.get(classOf[SslHandler])).map { sslHandler =>
      sslHandler.handshake()
    }
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cleanup = ctx.getAttachment
    if(cleanup != null) cleanup.asInstanceOf[() => Unit]()
    ctx.setAttachment(null)
  }

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    allChannels.add(e.getChannel)
  }


  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {

      case nettyHttpRequest: HttpRequest =>

        Play.logger.trace("Http request received by netty: " + nettyHttpRequest)
        val keepAlive = isKeepAlive(nettyHttpRequest)
        val websocketableRequest = websocketable(nettyHttpRequest)
        var nettyVersion = nettyHttpRequest.getProtocolVersion
        val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
        val rHeaders = getHeaders(nettyHttpRequest)

        def rRemoteAddress = e.getRemoteAddress match {
          case ra: java.net.InetSocketAddress => {
            val remoteAddress = ra.getAddress.getHostAddress
            (for {
              xff <- rHeaders.get(X_FORWARDED_FOR)
              app <- server.applicationProvider.get.right.toOption
              trustxforwarded <- app.configuration.getBoolean("trustxforwarded").orElse(Some(false))
              if remoteAddress == "127.0.0.1" || trustxforwarded
            } yield xff).getOrElse(remoteAddress)
          }
        }

        def tryToCreateRequest = {
          val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)
          createRequestHeader(parameters)
        }

        def createRequestHeader(parameters: Map[String, Seq[String]] = Map.empty[String, Seq[String]]) = {
          //mapping netty request to Play's
          val untaggedRequestHeader = new RequestHeader {
            val id = requestIDs.incrementAndGet
            val tags = Map.empty[String,String]
            def uri = nettyHttpRequest.getUri
            def path = nettyUri.getPath
            def method = nettyHttpRequest.getMethod.getName
            def version = nettyVersion.getText
            def queryString = parameters
            def headers = rHeaders
            lazy val remoteAddress = rRemoteAddress
            def username = None
          }
          untaggedRequestHeader
        }

        val (requestHeader, handler: Either[SimpleResult,(Handler,Application)]) = Exception
            .allCatch[RequestHeader].either(tryToCreateRequest)
            .fold(
              e => {
                val rh = createRequestHeader()
                val r = server.applicationProvider.get.fold(e => DefaultGlobal, a => a.global).onBadRequest(rh, e.getMessage)
                (rh, Left(r))
              },
              rh => server.getHandlerFor(rh) match {
                case directResult @ Left(_) => (rh, directResult)
                case Right((taggedRequestHeader, handler, application)) => (taggedRequestHeader, Right((handler, application)))
              }
            )

        // Call onRequestCompletion after all request processing is done. Protected with an AtomicBoolean to ensure can't be executed more than once.
        val alreadyClean = new java.util.concurrent.atomic.AtomicBoolean(false)
        def cleanup() {
          if (!alreadyClean.getAndSet(true)) {
            play.api.Play.maybeApplication.foreach(_.global.onRequestCompletion(requestHeader))            
          }
        }
        
        // attach the cleanup function to the channel context for after cleaning
        ctx.setAttachment(cleanup _)

        // It is a pre-requesite that we're using the http pipelining capabilities provided and that we have a
        // handler downstream from this one that produces these events.
        implicit val msgCtx = ctx
        implicit val oue = e.asInstanceOf[OrderedUpstreamMessageEvent]

        def cleanFlashCookie(result: SimpleResult): SimpleResult = {
          val header = result.header

          val flashCookie = {
            header.headers.get(SET_COOKIE)
            .map(Cookies.decode(_))
            .flatMap(_.find(_.name == Flash.COOKIE_NAME)).orElse {
              Option(requestHeader.flash).filterNot(_.isEmpty).map { _ =>
                Flash.discard.toCookie
              }
            }
          }

          flashCookie.map { newCookie =>
            result.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
          }.getOrElse(result)
        }

        handler match {
          //execute normal action
          case Right((action: EssentialAction, app)) =>
            val a = EssentialAction { rh =>
              Iteratee.flatten(action(rh).unflatten.map(_.it).recover {
                case error => Done(app.handleError(requestHeader, error),Input.Empty): Iteratee[Array[Byte],SimpleResult]
              })
            }
            handleAction(a, Some(app))

          case Right((ws @ WebSocket(f), app)) if (websocketableRequest.check) =>
            Play.logger.trace("Serving this request with: " + ws)
            val enumerator = websocketHandshake(ctx, nettyHttpRequest, e)(ws.frameFormatter)
            f(requestHeader)(enumerator, socketOut(ctx)(ws.frameFormatter))

          //handle bad websocket request
          case Right((WebSocket(_), app)) =>
            Play.logger.trace("Bad websocket request")
            val a = EssentialAction(_ => Done(Results.BadRequest,Input.Empty))
            handleAction(a,Some(app))

          case Left(e) =>
            Play.logger.trace("No handler, got direct result: " + e)
            val a = EssentialAction(_ => Done(e,Input.Empty))
            handleAction(a,None)

        }

        def handleAction(action: EssentialAction, app: Option[Application]){
          Play.logger.trace("Serving this request with: " + action)

          val bodyParser = Iteratee.flatten(
            scala.concurrent.Future(action(requestHeader))(play.api.libs.concurrent.Execution.defaultContext)
          )

          val expectContinue: Option[_] = requestHeader.headers.get("Expect").filter(_.equalsIgnoreCase("100-continue"))

          def feedBody[A](bodyParser: Iteratee[Array[Byte], A]) = if (nettyHttpRequest.isChunked) {

            val p: ChannelPipeline = ctx.getChannel().getPipeline()
            val result = newRequestBodyUpstreamHandler(bodyParser, { handler =>
              p.replace("handler", "handler", handler)
            }, {
              p.replace("handler", "handler", this)
            })

            result

          } else {

            val bodyEnumerator = {
              val body = {
                val cBuffer = nettyHttpRequest.getContent()
                val bytes = new Array[Byte](cBuffer.readableBytes())
                cBuffer.readBytes(bytes)
                bytes
              }
              Enumerator(body).andThen(Enumerator.enumInput(EOF))
            }

            bodyEnumerator |>> bodyParser
          }

          // This is an iteratee containing the result, and and the sequence number, which will be 1 if 100 continue
          // was sent
          val eventuallyResultIteratee = expectContinue match {
            case Some(_) => {
              bodyParser.unflatten.flatMap {
                case c @ Step.Cont(k) =>
                  sendDownstream(0, false, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
                  feedBody(bodyParser.map((_, 1)))
                case _ => {
                  // Ignore the body
                  Future.successful(bodyParser.map((_, 0)))
                }
              }
            }
            case None => feedBody(bodyParser.map((_, 0)))
          }

          val eventuallyResult = eventuallyResultIteratee.flatMap(it => it.run)

          val sent = eventuallyResult.recover {
            case error =>
              Play.logger.error("Cannot invoke the action, eventually got an error: " + error)
              e.getChannel.setReadable(true)
              (app.map(_.handleError(requestHeader, error)).getOrElse(DefaultGlobal.onError(requestHeader, error)), 0)
          }.flatMap {
            case (result, sequence) =>
              NettyResultStreamer.sendResult(cleanFlashCookie(result), !keepAlive, nettyVersion, sequence)
          }

          // Finally, clean up
          sent.map { _ =>
            cleanup()
            ctx.setAttachment(null)
          }
        }

      case unexpected => Play.logger.error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

    }
  }

  def socketOut[A](ctx: ChannelHandlerContext)(frameFormatter: play.api.mvc.WebSocket.FrameFormatter[A]): Iteratee[A, Unit] = {
    val channel = ctx.getChannel()
    val nettyFrameFormatter = frameFormatter.asInstanceOf[play.core.server.websocket.FrameFormatter[A]]

    def step(future: Option[ChannelFuture])(input: Input[A]): Iteratee[A, Unit] =
      input match {
        case El(e) => Cont(step(Some(channel.write(nettyFrameFormatter.toFrame(e)))))
        case e @ EOF => future.map(_.addListener(ChannelFutureListener.CLOSE)).getOrElse(channel.close()); Done((), e)
        case Empty => Cont(step(future))
      }

    Enumeratee.breakE[A](_ => !channel.isConnected()).transform(Cont(step(None)))
  }

  def getHeaders(nettyRequest: HttpRequest): Headers = {
    val pairs = nettyRequest.getHeaders.asScala.groupBy(_.getKey).mapValues(_.map(_.getValue))
    new Headers { val data = pairs.toSeq }
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)
                    (implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamMessageEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }
}
