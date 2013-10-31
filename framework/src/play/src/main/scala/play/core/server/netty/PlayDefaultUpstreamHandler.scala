package play.core.server.netty

import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.socket.nio._
import org.jboss.netty.handler.stream._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import org.jboss.netty.channel.group._
import java.util.concurrent._
import play.core._
import server.Server
import play.api._
import play.api.mvc._
import play.api.http.HeaderNames.X_FORWARDED_FOR
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._
import scala.collection.JavaConverters._

private[server] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup) extends SimpleChannelUpstreamHandler with Helpers with WebSocketHandler with RequestBodyHandler {

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    Logger.trace("Exception caught in Netty", e.getCause)
    e.getChannel.close()
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
        var version = nettyHttpRequest.getProtocolVersion
        val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
        val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)

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

        import org.jboss.netty.util.CharsetUtil;

        //mapping netty request to Play's

        val requestHeader = new RequestHeader {
          def uri = nettyHttpRequest.getUri
          def path = nettyUri.getPath
          def method = nettyHttpRequest.getMethod.getName
          def queryString = parameters
          def headers = rHeaders
          lazy val remoteAddress = rRemoteAddress
          def username = None
        }

        // converting netty response to play's
        val response = new Response {

          def handle(result: Result, closeConnection: Boolean) {
            result match {

              case AsyncResult(p) => p.extend1 {
                case Redeemed(v) => handle(v, closeConnection)
                case Thrown(e) => {
                  Logger("play").error("Waiting for a promise, but got an error: " + e.getMessage, e)
                  handle(Results.InternalServerError, closeConnection)
                }
              }

              case r @ SimpleResult(ResponseHeader(status, headers), body) if (!websocketableRequest.check) => {
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))

                Logger("play").trace("Sending simple result: " + r)

                // Set response headers
                headers.filterNot(_ == (CONTENT_LENGTH,"-1")).foreach {

                  // Fix a bug for Set-Cookie header. 
                  // Multiple cookies could be merged in a single header
                  // but it's not properly supported by some browsers
                  case (name @ play.api.http.HeaderNames.SET_COOKIE, value) => {
                    nettyResponse.setHeader(name, Cookies.decode(value).map { c => Cookies.encode(Seq(c)) }.asJava)
                  }

                  case (name, value) => nettyResponse.setHeader(name, value)
                }

                // Response header Connection: Keep-Alive is needed for HTTP 1.0
                if (!closeConnection && version == HttpVersion.HTTP_1_0) {
                  nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
                } else if (closeConnection && version == HttpVersion.HTTP_1_1) {
                  nettyResponse.setHeader(CONNECTION, CLOSE)
                }

                // Stream the result
                headers.get(CONTENT_LENGTH).map { contentLength =>

                  val writer: Function1[r.BODY_CONTENT, Promise[Unit]] = x => {
                    if (e.getChannel.isConnected())
                      NettyPromise(e.getChannel.write(ChannelBuffers.wrappedBuffer(r.writeable.transform(x))))
                        .extend1{ case Redeemed(()) => () ; case Thrown(ex) => Logger("play").debug(ex.toString)}
                    else Promise.pure(())
                  }

                  val bodyIteratee = {
                    val writeIteratee = Iteratee.fold1(
                      if (e.getChannel.isConnected())
                        NettyPromise( e.getChannel.write(nettyResponse))
                        .extend1{ case Redeemed(()) => () ; case Thrown(ex) => Logger("play").debug(ex.toString)}
                      else Promise.pure(()))((_, e: r.BODY_CONTENT) => writer(e))

                    Enumeratee.breakE[r.BODY_CONTENT](_ => !e.getChannel.isConnected()).transform(writeIteratee).mapDone { _ =>
                      if (e.getChannel.isConnected()) {
                        if (closeConnection) e.getChannel.close()
                      }
                    }
                  }

                  body(bodyIteratee)

                }.getOrElse {

                  // No Content-Length header specified, buffer in-memory
                  val channelBuffer = ChannelBuffers.dynamicBuffer(512)
                  val writer: Function2[ChannelBuffer, r.BODY_CONTENT, Unit] = (c, x) => c.writeBytes(r.writeable.transform(x))
                  val stringIteratee = Iteratee.fold(channelBuffer)((c, e: r.BODY_CONTENT) => { writer(c, e); c })
                  val p = body |>> stringIteratee
                  p.flatMap(i => i.run)
                    .onRedeem { buffer =>
                      nettyResponse.setHeader(CONTENT_LENGTH, channelBuffer.readableBytes)
                      nettyResponse.setContent(buffer)
                      val f = e.getChannel.write(nettyResponse)
                      if (closeConnection) f.addListener(ChannelFutureListener.CLOSE)
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


                val writer: Function1[r.BODY_CONTENT, Promise[Unit]] = x => {
                    if (e.getChannel.isConnected())
                      NettyPromise(e.getChannel.write(new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(r.writeable.transform(x)))))
                        .extend1{ case Redeemed(()) => () ; case Thrown(ex) => Logger("play").debug(ex.toString)}
                    else Promise.pure(())
                  }

                  val chunksIteratee = {
                    val writeIteratee = Iteratee.fold1(
                      if (e.getChannel.isConnected())
                        NettyPromise( e.getChannel.write(nettyResponse))
                        .extend1{ case Redeemed(()) => () ; case Thrown(ex) => Logger("play").debug(ex.toString)}
                      else Promise.pure(()))((_, e: r.BODY_CONTENT) => writer(e))


                  Enumeratee.breakE[r.BODY_CONTENT](_ => !e.getChannel.isConnected())(writeIteratee).mapDone { _ =>
                    if (e.getChannel.isConnected()) {
                      val f = e.getChannel.write(HttpChunk.LAST_CHUNK);
                      if (closeConnection) f.addListener(ChannelFutureListener.CLOSE)
                    }
                  }
                  }

                chunks(chunksIteratee)

              }

              case _ =>
                val channelBuffer = ChannelBuffers.dynamicBuffer(512)
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(500))
                nettyResponse.setContent(channelBuffer)
                nettyResponse.setHeader(CONTENT_LENGTH, 0)
                val f = e.getChannel.write(nettyResponse)
                if (closeConnection) f.addListener(ChannelFutureListener.CLOSE)
            }
          }
        }
        // get handler for request
        val handler = server.getHandlerFor(requestHeader)

        handler match {

          //execute normal action
          case Right((action: Action[_], app)) => {

            Logger("play").trace("Serving this request with: " + action)
            type BODY = action.BODY_CONTENT

            val bodyParser = Iteratee.flatten(server.getBodyParser[BODY](requestHeader, action.parser))

            val expectContinue: Option[_] = requestHeader.headers.get("Expect").filter(_.equalsIgnoreCase("100-continue"))

            // Regardless of whether the client is expecting 100 continue or not, we need to feed the body here in the
            // Netty thread, so that the handler is replaced in this thread, so that if the client does start sending
            // body chunks (which it might according to the HTTP spec if we're slow to respond), we can handle them.

            val eventuallyResultOrBody: Promise[Either[Result, BODY]] = if (nettyHttpRequest.isChunked) {

              val ( result, handler) = newRequestBodyHandler(bodyParser, allChannels, server)

              val p: ChannelPipeline = ctx.getChannel().getPipeline()
              p.replace("handler", "handler", handler)

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

              (bodyEnumerator |>> bodyParser).flatMap(_.run)
            }

            // A promise containing the result and whether the connection should be closed.
            // The connection should be closed if a 100 continue expectation wasn't sent.
            val eventuallyResultOrBodyAndClose: Promise[(Either[Result, BODY], Boolean)] = expectContinue match {
              case Some(_) => {
                bodyParser.fold(
                  (resultOrBody, _) => Promise.pure((resultOrBody, true)),
                  k => {
                    e.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
                    eventuallyResultOrBody.map((_, !keepAlive))
                  },
                  (msg, _) => Promise.pure(sys.error(msg): (Either[Result, BODY], Boolean))
                )
              }
              case None => eventuallyResultOrBody.map((_, !keepAlive))
            }

            val eventuallyResultOrRequest = eventuallyResultOrBodyAndClose.map {
              case (resultOrBody, close) => (resultOrBody.right.map(b =>
                new Request[BODY] {
                  def uri = nettyHttpRequest.getUri
                  def path = nettyUri.getPath
                  def method = nettyHttpRequest.getMethod.getName
                  def queryString = parameters
                  def headers = rHeaders
                  lazy val remoteAddress = rRemoteAddress
                  def username = None
                  val body = b
                }
              ), close)
            }

            eventuallyResultOrRequest.extend(_.value match {
              case Redeemed((Left(result), close)) => {
                Logger("play").trace("Got direct result from the BodyParser: " + result)
                response.handle(result, close)
              }
              case Redeemed((Right(request), close)) => {
                Logger("play").trace("Invoking action with request: " + request)
                server.invoke(request, response, action.asInstanceOf[Action[BODY]], app, close)
              }
              case error => {
                Logger("play").error("Cannot invoke the action, eventually got an error: " + error)
                response.handle(Results.InternalServerError, true)
              }
            })

          }

          //execute websocket action
          case Right((ws @ WebSocket(f), app)) if (websocketableRequest.check) => {

            Logger("play").trace("Serving this request with: " + ws)

            try {
              val enumerator = websocketHandshake(ctx, nettyHttpRequest, e)(ws.frameFormatter)
              f(requestHeader)(enumerator, socketOut(ctx)(ws.frameFormatter))
            } catch {
              case e => e.printStackTrace
            }
          }

          //handle bad websocket request
          case Right((WebSocket(_), _)) => {

            Logger("play").trace("Bad websocket request")

            response.handle(Results.BadRequest, true)
          }

          //handle errors
          case Left(e) => {

            Logger("play").trace("No handler, got direct result: " + e)

            response.handle(e, keepAlive)
          }

        }

      case unexpected => Logger("play").error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

    }
  }

}
