package play.core.server.netty

import scala.language.reflectiveCalls

import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import org.jboss.netty.handler.ssl._

import org.jboss.netty.channel.group._
import play.core._
import server.Server
import play.api._
import play.api.mvc._
import play.api.http.HeaderNames.X_FORWARDED_FOR
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.util.control.Exception
import com.typesafe.netty.http.pipelining.{OrderedDownstreamMessageEvent, OrderedUpstreamMessageEvent}


private[server] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup) extends SimpleChannelUpstreamHandler with Helpers with WebSocketHandler with RequestBodyHandler {

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

        Logger("play").trace("Http request received by netty: " + nettyHttpRequest)
        val keepAlive = isKeepAlive(nettyHttpRequest)
        val websocketableRequest = websocketable(nettyHttpRequest)
        var nettyVersion = nettyHttpRequest.getProtocolVersion
        val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
        val rHeaders = getHeaders(nettyHttpRequest)
        val rCookies = getCookies(nettyHttpRequest)

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

        val (untaggedRequestHeader, handler) = Exception
            .allCatch[RequestHeader].either(tryToCreateRequest)
            .fold(
               e => {
                 val rh = createRequestHeader()
                 val r = server.applicationProvider.get.fold(e => DefaultGlobal, a => a.global).onBadRequest(rh, e.getMessage)
                 (rh, Left(r))
               },
               rh => (rh, server.getHandlerFor(rh)))

        // tag request if necessary
        val requestHeader = handler.right.toOption.map({
          case (h: RequestTaggingHandler, _) => h.tagRequest(untaggedRequestHeader)
          case _ => untaggedRequestHeader
        }).getOrElse(untaggedRequestHeader)

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

        // converting netty response to play's
        val response = new Response {

          def handle(result: Result) {
            result match {

              case AsyncResult(p) => p.extend1 {
                case Redeemed(v) => handle(v)
                case Thrown(e) => {
                  server.applicationProvider.get match {
                    case Right(app) => handle(app.handleError(requestHeader, e))
                    case Left(_) => handle(Results.InternalServerError)
                  }
                }
              }

              case r @ SimpleResult(ResponseHeader(status, headers), body) if (!websocketableRequest.check) => {
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))

                Logger("play").trace("Sending simple result: " + r)

                // Set response headers
                headers.filterNot(_ == (CONTENT_LENGTH, "-1")).foreach {

                  // Fix a bug for Set-Cookie header. 
                  // Multiple cookies could be merged in a single header
                  // but it's not properly supported by some browsers
                  case (name @ play.api.http.HeaderNames.SET_COOKIE, value) => {
                    nettyResponse.setHeader(name, Cookies.decode(value).map { c => Cookies.encode(Seq(c)) }.asJava)
                  }

                  case (name, value) => nettyResponse.setHeader(name, value)
                }

                // Response header Connection: Keep-Alive is needed for HTTP 1.0
                if (keepAlive && nettyVersion == HttpVersion.HTTP_1_0) {
                  nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
                }

                // Stream the result
                headers.get(CONTENT_LENGTH).map { contentLength =>
                  val bodyIteratee = {
                    def step(subsequence: Int)(in: Input[r.BODY_CONTENT]): Iteratee[r.BODY_CONTENT, Unit] = in match {
                      case Input.El(x) =>
                        val b = ChannelBuffers.wrappedBuffer(r.writeable.transform(x))
                        nextWhenComplete(sendDownstream(subsequence, false, b), step(subsequence + 1))
                      case Input.Empty =>
                        Cont(step(subsequence))
                      case Input.EOF =>
                        sendDownstream(subsequence, true, ChannelBuffers.EMPTY_BUFFER)
                        Done(())
                    }
                    nextWhenComplete(sendDownstream(0, false, nettyResponse), step(1))
                  }

                  (body |>>> bodyIteratee).extend1 {
                    case Redeemed(_) =>
                      cleanup()
                      ctx.setAttachment(null)
                      if (!keepAlive) Channels.close(e.getChannel)
                    case Thrown(ex) =>
                      Logger("play").debug(ex.toString)
                      Channels.close(e.getChannel)
                  }
                }.getOrElse {

                  // No Content-Length header specified, buffer in-memory
                  val channelBuffer = ChannelBuffers.dynamicBuffer(512)
                  val writer: Function2[ChannelBuffer, r.BODY_CONTENT, Unit] = (c, x) => c.writeBytes(r.writeable.transform(x))
                  val stringIteratee = Iteratee.fold(channelBuffer)((c, e: r.BODY_CONTENT) => { writer(c, e); c })(internalExecutionContext)
                  val p = (body |>>> Enumeratee.grouped(stringIteratee) &>> Cont { 
                    case Input.El(buffer) =>
                      nettyResponse.setHeader(CONTENT_LENGTH, channelBuffer.readableBytes)
                      nettyResponse.setContent(buffer)
                      val f = sendDownstream(0, true, nettyResponse)
                      if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
                      val p = NettyPromise(f)
                      Iteratee.flatten(p.map(_ => Done(1, Input.Empty:Input[ChannelBuffer])))

                    case other => Error("unexpected input",other)
                  })
                  p.extend1 {
                    case Redeemed(_) =>
                      cleanup()
                      ctx.setAttachment(null)
                      if (!keepAlive) Channels.close(e.getChannel)
                    case Thrown(ex) =>
                      Logger("play").debug(ex.toString)
                      Channels.close(e.getChannel)
                  }
                }
              }

              case r @ ChunkedResult(ResponseHeader(status, headers), chunks) => {

                Logger("play").trace("Sending chunked result: " + r)

                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))

                // Copy headers to netty response
                headers.foreach {

                  // Fix a bug for Set-Cookie header. 
                  // Multiple cookies could be merged in a single header
                  // but it's not properly supported by some browsers
                  case (name @ play.api.http.HeaderNames.SET_COOKIE, value) => {

                    import scala.collection.JavaConverters._
                    import play.api.mvc._

                    nettyResponse.setHeader(name, Cookies.decode(value).map { c => Cookies.encode(Seq(c)) }.asJava)

                  }

                  case (name, value) => nettyResponse.setHeader(name, value)
                }

                nettyResponse.setHeader(TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED)
                nettyResponse.setChunked(true)
                val bodyIteratee = {
                  def step(subsequence: Int)(in:Input[r.BODY_CONTENT]): Iteratee[r.BODY_CONTENT, Unit] = in match {
                    case Input.El(x) =>
                      val b = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(r.writeable.transform(x)))
                      nextWhenComplete(sendDownstream(subsequence, false, b), step(subsequence + 1))
                    case Input.Empty =>
                      Cont(step(subsequence))
                    case Input.EOF =>
                      val f = sendDownstream(subsequence, true, HttpChunk.LAST_CHUNK)
                      val p = NettyPromise(f)
                      Iteratee.flatten(p.map(_ => Done(())))
                  }
                  nextWhenComplete(sendDownstream(0, false, nettyResponse), step(1))
                }

                chunks apply bodyIteratee.map[Unit] { _ =>
                  cleanup()
                  ctx.setAttachment(null)
                  if (!keepAlive) Channels.close(e.getChannel)
                }(internalExecutionContext)
              }

              case _ =>
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(500))
                nettyResponse.setHeader(CONTENT_LENGTH, 0)
                val f = sendDownstream(0, true, nettyResponse)
                if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
            }
          }
        }

        def cleanFlashCookie(r:PlainResult):Result = {
          val header = r.header

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
            r.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
          }.getOrElse(r)
        }

        handler match {
          //execute normal action
          case Right((action: EssentialAction, app)) =>
            val a = EssentialAction{ rh =>
              Iteratee.flatten(action(rh).map {
                case r: PlainResult => cleanFlashCookie(r)
                case a:AsyncResult => a.transform(cleanFlashCookie)(internalExecutionContext)
              }(internalExecutionContext).unflatten.extend1{
                case Redeemed(it) => it.it
                case Thrown(e) => Done(app.handleError(requestHeader, e),Input.Empty): Iteratee[Array[Byte],Result]
              }(internalExecutionContext))
            }
            handleAction(a,Some(app))

          case Right((ws @ WebSocket(f), app)) if (websocketableRequest.check) =>
            Logger("play").trace("Serving this request with: " + ws)

            try {
              val enumerator = websocketHandshake(ctx, nettyHttpRequest, e)(ws.frameFormatter)
              f(requestHeader)(enumerator, socketOut(ctx)(ws.frameFormatter))
            } catch {
              case NonFatal(e) => e.printStackTrace()
            }

          //handle bad websocket request
          case Right((WebSocket(_), app)) =>
            Logger("play").trace("Bad websocket request")
            val a = EssentialAction(_ => Done(Results.BadRequest,Input.Empty))
            handleAction(a,Some(app))

          case Left(e) =>
            Logger("play").trace("No handler, got direct result: " + e)
            val a = EssentialAction(_ => Done(e,Input.Empty))
            handleAction(a,None)

        }

        def handleAction(a:EssentialAction,app:Option[Application]){
          Logger("play").trace("Serving this request with: " + a)

          val filteredAction = app.map(_.global).getOrElse(DefaultGlobal).doFilter(a)

          val eventuallyBodyParser = scala.concurrent.Future(filteredAction(requestHeader))(play.api.libs.concurrent.Execution.defaultContext)

          requestHeader.headers.get("Expect").filter(_ == "100-continue").foreach { _ =>
            eventuallyBodyParser.flatMap(_.unflatten)(internalExecutionContext).map {
              case Step.Cont(k) =>
                sendDownstream(0, true, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
              case _ =>
            }(internalExecutionContext)
          }

          val eventuallyResultIteratee = if (nettyHttpRequest.isChunked) {

            val (result, handler) = newRequestBodyHandler(eventuallyBodyParser, allChannels, server)

            val p: ChannelPipeline = ctx.getChannel().getPipeline()
            p.replace("handler", "handler", handler)

            result

          } else {

            lazy val bodyEnumerator = {
              val body = {
                val cBuffer = nettyHttpRequest.getContent()
                val bytes = new Array[Byte](cBuffer.readableBytes())
                cBuffer.readBytes(bytes)
                bytes
              }
              Enumerator(body).andThen(Enumerator.enumInput(EOF))
            }

            eventuallyBodyParser.flatMap(it => bodyEnumerator |>> it)(internalExecutionContext): scala.concurrent.Future[Iteratee[Array[Byte], Result]]

          }


          val eventuallyResult = eventuallyResultIteratee.flatMap(it => it.run)(internalExecutionContext)
          eventuallyResult.extend1 {
            case Redeemed(r) => response.handle(r)

            case Thrown(error) =>
              Logger("play").error("Cannot invoke the action, eventually got an error: " + error)
              response.handle( app.map(_.handleError(requestHeader, error)).getOrElse(DefaultGlobal.onError(requestHeader, error)))
              e.getChannel.setReadable(true)
          }(internalExecutionContext)
        }

      case unexpected => Logger("play").error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

    }
  }

  def sendDownstream(subSequence: Int, last: Boolean, message: Object)
                    (implicit ctx: ChannelHandlerContext, oue: OrderedUpstreamMessageEvent) = {
    val ode = new OrderedDownstreamMessageEvent(oue, subSequence, last, message)
    ctx.sendDownstream(ode)
    ode.getFuture
  }

  def nextWhenComplete[E](future: ChannelFuture, step: (Input[E]) => Iteratee[E, Unit])
                      (implicit ctx: ChannelHandlerContext)
                      : Iteratee[E, Unit] = {
    Iteratee.flatten(
      NettyPromise(future)
        .map[Iteratee[E,Unit]](_ => if (ctx.getChannel.isConnected()) Cont(step) else Done((), Input.Empty))(internalExecutionContext))
  }

}
