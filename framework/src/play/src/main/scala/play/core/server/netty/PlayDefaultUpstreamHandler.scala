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
import org.jboss.netty.handler.ssl._

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
import scala.collection.immutable
import java.security.cert.Certificate
import collection.immutable.Nil
import javax.net.ssl.SSLException

object PlayDefaultUpstreamHandler {
  val logger = Logger("play")
}

private[server] class PlayDefaultUpstreamHandler(server: Server, allChannels: DefaultChannelGroup) extends SimpleChannelUpstreamHandler with Helpers with WebSocketHandler with RequestBodyHandler {
  import PlayDefaultUpstreamHandler.logger

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.warn("Exception caught in Netty", e.getCause)
    e.getChannel.close()
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    Option(ctx.getPipeline.get(classOf[SslHandler])).map { sslHandler =>
      sslHandler.handshake()
    }
  }

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    allChannels.add(e.getChannel)
  }

  val emptySeq: immutable.IndexedSeq[Certificate] = Nil.toIndexedSeq
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

    trait certs { req: RequestHeader =>

      def certs: Promise[IndexedSeq[Certificate]] = {
        import org.jboss.netty.handler.ssl.SslHandler
        import scala.util.control.Exception._
        import javax.net.ssl.SSLPeerUnverifiedException
        val sslCatcher = catching(classOf[SSLPeerUnverifiedException])

        val res: Option[Promise[IndexedSeq[Certificate]]] = Option(ctx.getPipeline.get(classOf[SslHandler])).flatMap { sslh =>
          sslCatcher.opt {
            logger.info("checking for certs in ssl session")
            val res = sslh.getEngine.getSession.getPeerCertificates.toIndexedSeq[Certificate]
            Promise.pure[IndexedSeq[Certificate]](res)
          } orElse {
            logger.info("attempting to request certs from client")
            //need to make use of the certificate sessions in the setup process
            //see http://stackoverflow.com/questions/8731157/netty-https-tls-session-duration-why-is-renegotiation-needed
            sslh.setEnableRenegotiation(true); //does this have to be done on every request?
            req.headers.get("User-Agent") match {
              case Some(agent) if needAuth(agent) => sslh.getEngine.setNeedClientAuth(true)
              case _ => sslh.getEngine.setWantClientAuth(true)
            }
            val future = sslh.handshake()
            future.await()
            val r: Promise[IndexedSeq[Certificate]] = NettyPromise(future).extend{ p=>
                sslh.getEngine.getSession.getPeerCertificates.toIndexedSeq[Certificate]
            }
            Some(r)
          }
         }
        logger.info("returning Promise")
        res.getOrElse(Promise.pure(throw new SSLException("No SSLHandler!")))
      }

      /**
       *  Some agents do not send client certificates unless required by a NEED.
       *
       *  This is a problem for them, as it ends up breaking the SSL connection for agents that do not send a cert.
       *  For human agents this is a bigger problem, as one would rather send them a message of explanation for what
       *  went wrong, with perhaps pointers to other methods  of authentication, rather than showing an ugly connection
       *  broken/dissallowed window ) For Opera and AppleWebKit browsers (Safari) authentication requests should
       *  therefore be done over javascript, on resources that NEED a cert, and which can fail cleanly with a
       *  javascript exception.  )
       *
       *  It would be useful if this could be updated by server from time to  time from a file on the internet,
       *  so that changes to browsers could update server behavior.
       *
       */
      def needAuth(agent: String): Boolean =
        (agent contains "Java")  | (agent contains "AppleWebKit")  |  (agent contains "Opera") | (agent contains "libcurl")

    }

    e.getMessage match {

      case nettyHttpRequest: HttpRequest =>

        logger.trace("Http request received by netty: " + nettyHttpRequest)

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

        val requestHeader = new RequestHeader with certs {
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

          def handle(result: Result) {
            result match {

              case AsyncResult(p) => p.extend1 {
                case Redeemed(v) => handle(v)
                case Thrown(e) => {
                  logger.error("Waiting for a promise, but got an error: " + e.getMessage, e)
                  handle(Results.InternalServerError)
                }
              }

              case r @ SimpleResult(ResponseHeader(status, headers), body) if (!websocketableRequest.check) => {
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))

                logger.trace("Sending simple result: " + r)

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
                if (keepAlive && version == HttpVersion.HTTP_1_0) {
                  nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
                }

                // Stream the result
                headers.get(CONTENT_LENGTH).map { contentLength =>

                  val writer: Function1[r.BODY_CONTENT, Promise[Unit]] = x => {
                    if (e.getChannel.isConnected())
                      NettyPromise(e.getChannel.write(ChannelBuffers.wrappedBuffer(r.writeable.transform(x))))
                        .extend1 { case Redeemed(()) => (); case Thrown(ex) => Logger("play").debug(ex.toString) }
                    else Promise.pure(())
                  }

                  val bodyIteratee = {
                    val writeIteratee = Iteratee.fold1(
                      if (e.getChannel.isConnected())
                        NettyPromise(e.getChannel.write(nettyResponse))
                        .extend1 { case Redeemed(()) => (); case Thrown(ex) => Logger("play").debug(ex.toString) }
                      else Promise.pure(()))((_, e: r.BODY_CONTENT) => writer(e))

                    Enumeratee.breakE[r.BODY_CONTENT](_ => !e.getChannel.isConnected()).transform(writeIteratee).mapDone { _ =>
                      if (e.getChannel.isConnected()) {
                        if (!keepAlive) e.getChannel.close()
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
                      if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
                    }

                }

              }

              case r @ ChunkedResult(ResponseHeader(status, headers), chunks) => {

                logger.trace("Sending chunked result: " + r)

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
                      .extend1 { case Redeemed(()) => (); case Thrown(ex) => Logger("play").debug(ex.toString) }
                  else Promise.pure(())
                }

                val chunksIteratee = {
                  val writeIteratee = Iteratee.fold1(
                    if (e.getChannel.isConnected())
                      NettyPromise(e.getChannel.write(nettyResponse))
                      .extend1 { case Redeemed(()) => (); case Thrown(ex) => Logger("play").debug(ex.toString) }
                    else Promise.pure(()))((_, e: r.BODY_CONTENT) => writer(e))

                  Enumeratee.breakE[r.BODY_CONTENT](_ => !e.getChannel.isConnected())(writeIteratee).mapDone { _ =>
                    if (e.getChannel.isConnected()) {
                      val f = e.getChannel.write(HttpChunk.LAST_CHUNK);
                      if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
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
                if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
            }
          }
        }
        // get handler for request
        val handler = server.getHandlerFor(requestHeader)

        handler match {

          //execute normal action
          case Right((action: Action[_], app)) => {

            logger.trace("Serving this request with: " + action)

            val bodyParser = action.parser

            val eventuallyBodyParser = server.getBodyParser[action.BODY_CONTENT](requestHeader, bodyParser)

            val _ =
              eventuallyBodyParser.flatMap { bodyParser =>

                requestHeader.headers.get("Expect") match {
                  case Some("100-continue") => {
                    bodyParser.pureFold {
                      case Step.Done(_, _) => ()
                      case Step.Cont(k) => {
                        val continue = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)
                        e.getChannel.write(continue)

                      }
                      case Step.Error(_, _) => ()
                    }
                  }
                  case _ => Promise.pure()
                }
              }

            val eventuallyResultOrBody = if (nettyHttpRequest.isChunked) {

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

              eventuallyBodyParser.flatMap(it => bodyEnumerator |>> it): Promise[Iteratee[Array[Byte], Either[Result, action.BODY_CONTENT]]]

            }

            val eventuallyResultOrRequest =
              eventuallyResultOrBody
                .flatMap(it => it.run)
                .map {
                  _.right.map(b =>
                    new Request[action.BODY_CONTENT] with certs {
                      def uri = nettyHttpRequest.getUri
                      def path = nettyUri.getPath
                      def method = nettyHttpRequest.getMethod.getName
                      def queryString = parameters
                      def headers = rHeaders
                      lazy val remoteAddress = rRemoteAddress
                      def username = None
                      val body = b
                    })
                }

            eventuallyResultOrRequest.extend(_.value match {
              case Redeemed(Left(result)) => {
                logger.trace("Got direct result from the BodyParser: " + result)
                response.handle(result)
              }
              case Redeemed(Right(request)) => {
                logger.trace("Invoking action with request: " + request)
                server.invoke(request, response, action.asInstanceOf[Action[action.BODY_CONTENT]], app)
              }
              case error => {
                logger.error("Cannot invoke the action, eventually got an error: " + error)
                response.handle(Results.InternalServerError)
                e.getChannel.setReadable(true)
              }
            })

          }

          //execute websocket action
          case Right((ws @ WebSocket(f), app)) if (websocketableRequest.check) => {

            logger.trace("Serving this request with: " + ws)

            try {
              val enumerator = websocketHandshake(ctx, nettyHttpRequest, e)(ws.frameFormatter)
              f(requestHeader)(enumerator, socketOut(ctx)(ws.frameFormatter))
            } catch {
              case e => e.printStackTrace
            }
          }

          //handle bad websocket request
          case Right((WebSocket(_), _)) => {

            logger.trace("Bad websocket request")

            response.handle(Results.BadRequest)
          }

          //handle errors
          case Left(e) => {

            logger.trace("No handler, got direct result: " + e)

            response.handle(e)
          }

        }

      case unexpected => Logger("play").error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

    }
  }

}
