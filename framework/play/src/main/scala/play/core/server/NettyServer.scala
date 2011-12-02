package play.core.server

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
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder

import org.jboss.netty.channel.group._
import java.util.concurrent._

import play.core._
import play.core.server.websocket._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.collection.JavaConverters._

class NettyServer(appProvider: ApplicationProvider, host: String, port: Int, allowKeepAlive: Boolean = true) extends Server {

  def applicationProvider = appProvider

  val bootstrap = new ServerBootstrap(
    new org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()))

  val allChannels = new DefaultChannelGroup

  class PlayDefaultUpstreamHandler extends SimpleChannelUpstreamHandler {

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      e.getChannel.close()
    }

    private def isWebSocket(request: HttpRequest) =
      HttpHeaders.Values.UPGRADE.equalsIgnoreCase(request.getHeader(CONNECTION)) &&
        HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.UPGRADE))

    private def websocketHandshake(ctx: ChannelHandlerContext, req: HttpRequest, e: MessageEvent): Enumerator[String] = {

      WebSocketHandshake.shake(ctx, req)

      val (enumerator, handler) = newWebSocketInHandler()
      val p: ChannelPipeline = ctx.getChannel().getPipeline();
      p.replace("handler", "handler", handler);

      enumerator
    }

    private def socketOut[A](ctx: ChannelHandlerContext)(writeable: Writeable[A]): Iteratee[A, Unit] = {
      val channel = ctx.getChannel()

      def step(future: Option[ChannelFuture])(input: Input[A]): Iteratee[A, Unit] =
        input match {
          // FIXME: what is we want something else than text?
          case El(e) => Cont(step(Some(channel.write(new TextFrame(true, 0, new String(writeable.transform(e)))))))
          case e @ EOF => future.map(_.addListener(ChannelFutureListener.CLOSE)).getOrElse(channel.close()); Done((), e)
          case Empty => Cont(step(future))
        }

      Cont(step(None))
    }

    private def newRequestBodyHandler[R](it: Iteratee[Array[Byte], Either[Result, R]]): (Promise[Either[String, Iteratee[Array[Byte], Either[Result, R]]]], SimpleChannelUpstreamHandler) = {
      var redeemed = false
      var iteratee: Iteratee[Array[Byte], Either[Result, R]] = it
      var p = Promise[Either[String, Iteratee[Array[Byte], Either[Result, R]]]]()
      def pushChunk(ctx: ChannelHandlerContext, chunk: Input[Array[Byte]]) {
        if (!redeemed) {
          val next = iteratee.pureFlatFold[Array[Byte], Either[Result, R]](
            (_, _) => iteratee,
            k => k(chunk),
            (_, _) => iteratee)
          iteratee = next

          next.pureFold(
            (a, e) => if (!redeemed) { p.redeem(Right(next)); iteratee = null; p = null; redeemed = true },
            k => (),
            (msg, e) => if (!redeemed) { p.redeem(Left(msg)); iteratee = null; p = null; redeemed = true })
        }
      }

      (p, new SimpleChannelUpstreamHandler {
        override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
          e.getMessage match {
            case chunk: HttpChunk if !chunk.isLast() =>
              val cBuffer = chunk.getContent()
              val bytes = new Array[Byte](cBuffer.readableBytes())
              cBuffer.readBytes(bytes)
              pushChunk(ctx, El(bytes))
            case chunk: HttpChunk if chunk.isLast() => pushChunk(ctx, EOF)

          }
        }

        override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
          e.getCause().printStackTrace();
          e.getChannel().close(); /*really? */
        }
        override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
          pushChunk(ctx, EOF)
        }

      })

    }

    private def newWebSocketInHandler() = {

      val enumerator = new Enumerator[String] {
        val iterateeAgent = Agent[Option[Iteratee[String, Any]]](None)
        private val promise: Promise[Iteratee[String, Any]] with Redeemable[Iteratee[String, Any]] = Promise[Iteratee[String, Any]]()

        def apply[R, EE >: String](i: Iteratee[EE, R]) = {
          iterateeAgent.send(_.orElse(Some(i.asInstanceOf[Iteratee[String, Any]])))
          promise.asInstanceOf[Promise[Iteratee[EE, R]]]
        }

        def frameReceived(ctx: ChannelHandlerContext, input: Input[String]) {
          iterateeAgent.send(iteratee =>
            iteratee.map(it => it.flatFold(
              (a, e) => { error("Getting messages on a supposedly closed socket? frame: " + input) },
              k => {
                val next = k(input)
                next.fold(
                  (a, e) => {
                    ctx.getChannel().disconnect();
                    iterateeAgent.close();
                    promise.redeem(next);
                    println("cleaning for channel " + ctx.getChannel());
                    Promise.pure(next)
                  },
                  _ => Promise.pure(next),
                  (msg, e) => { /* deal with error, maybe close the socket */ Promise.pure(next) })
              },
              (err, e) => /* handle error, maybe close the socket */ Promise.pure(it))))
        }
      }

      (enumerator,
        new SimpleChannelUpstreamHandler {

          override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
            e.getMessage match {
              // FIXME: This should not be a string in the future
              case frame: BinaryFrame => enumerator.frameReceived(ctx, El(new String(frame.binaryData.array, "UTF-8")))
              case frame: CloseFrame => enumerator.frameReceived(ctx, EOF)
              case frame: TextFrame => enumerator.frameReceived(ctx, El(frame.getTextData))
            }
          }

          override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
            e.getCause().printStackTrace();
            e.getChannel().close();
          }
          override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
            enumerator.frameReceived(ctx, EOF)
            println("disconnecting socket")
            println("disconnected socket")
          }
        })

    }

    private def getHeaders(nettyRequest: HttpRequest): Headers = {

      val headers: Map[String, Seq[String]] = nettyRequest.getHeaderNames.asScala.map { key =>
        key.toUpperCase -> nettyRequest.getHeaders(key).asScala
      }.toMap

      new Headers {
        def getAll(key: String) = headers.get(key.toUpperCase).flatten.toSeq
        override def toString = headers.toString
      }

    }

    private def getCookies(nettyRequest: HttpRequest): Cookies = {

      val cookies: Map[String, play.api.mvc.Cookie] = getHeaders(nettyRequest).get(play.api.http.HeaderNames.COOKIE).map { cookiesHeader =>
        new CookieDecoder().decode(cookiesHeader).asScala.map { c =>
          c.getName -> play.api.mvc.Cookie(
            c.getName, c.getValue, c.getMaxAge, Option(c.getPath).getOrElse("/"), Option(c.getDomain), c.isSecure, c.isHttpOnly)
        }.toMap
      }.getOrElse(Map.empty)

      new Cookies {
        def get(name: String) = cookies.get(name)
        override def toString = cookies.toString
      }

    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {

      allChannels.add(e.getChannel)

      e.getMessage match {
        case nettyHttpRequest: HttpRequest =>
          val keepAlive = allowKeepAlive && nettyHttpRequest.isKeepAlive
          var version = nettyHttpRequest.getProtocolVersion
          val nettyUri = new QueryStringDecoder(nettyHttpRequest.getUri)
          val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)

          val rHeaders = getHeaders(nettyHttpRequest)
          val rCookies = getCookies(nettyHttpRequest)

          import org.jboss.netty.util.CharsetUtil;

          val requestHeader = new RequestHeader {
            def uri = nettyHttpRequest.getUri
            def path = nettyUri.getPath
            def method = nettyHttpRequest.getMethod.getName
            def queryString = parameters
            def headers = rHeaders
            def cookies = rCookies
            def username = None
          }

          val response = new Response {
            def handle(result: Result) = result match {

              case AsyncResult(p) => p.extend1 {
                case Redeemed(v) => handle(v)
                case Thrown(e) => {
                  Logger("play").error("Waiting for a promise, but got an error: " + e.getMessage, e)
                  handle(Results.InternalServerError)
                }
              }

              case _ if (isWebSocket(nettyHttpRequest)) => handle(Results.BadRequest)

              case r @ SimpleResult(ResponseHeader(status, headers), body) =>
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
                headers.foreach {

                  // Fix a bug for Set-Cookie header.
                  // Multiple cookies could be merge in a single header
                  // but it's not properly supported by some browsers
                  case (name @ play.api.http.HeaderNames.SET_COOKIE, value) => {

                    import scala.collection.JavaConverters._
                    import play.api.mvc._

                    nettyResponse.setHeader(name, Cookies.decode(value).map { c => Cookies.encode(Seq(c)) }.asJava)

                  }

                  case (name, value) => nettyResponse.setHeader(name, value)
                }
                val channelBuffer = ChannelBuffers.dynamicBuffer(512)
                val writer: Function2[ChannelBuffer, r.BODY_CONTENT, Unit] = (c, x) => c.writeBytes(r.writeable.transform(x))
                val stringIteratee = Iteratee.fold(channelBuffer)((c, e: r.BODY_CONTENT) => { writer(c, e); c })
                val p = stringIteratee <<: body
                p.flatMap(i => i.run)
                  .onRedeem { buffer =>
                    nettyResponse.setContent(buffer)
                    if (keepAlive) {
                      nettyResponse.setHeader(CONTENT_LENGTH, nettyResponse.getContent.readableBytes)
                      if (version == HttpVersion.HTTP_1_0) {
                        // Response header Connection: Keep-Alive is needed for HTTP 1.0
                        nettyResponse.setHeader(CONNECTION, KEEP_ALIVE)
                      }
                    }
                    val f = e.getChannel.write(nettyResponse)
                    if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
                  }

              case r @ ChunkedResult(ResponseHeader(status, headers), chunks) =>
                val nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
                headers.foreach {

                  // Fix a bug for Set-Cookie header.
                  // Multiple cookies could be merge in a single header
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

                val writer: Function1[r.BODY_CONTENT, ChannelFuture] = x => e.getChannel.write(new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(r.writeable.transform(x))))

                val chunksIteratee = Enumeratee.breakE[r.BODY_CONTENT](_ => !e.getChannel.isConnected())(Iteratee.fold(e.getChannel.write(nettyResponse))((_, e: r.BODY_CONTENT) => writer(e))).mapDone { _ =>
                  val f = e.getChannel.write(HttpChunk.LAST_CHUNK);
                  if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)
                }

                chunks(chunksIteratee)

            }
          }

          val handler = getHandlerFor(requestHeader)

          handler match {
            case Right((action: Action[_], app)) => {

              val bodyParser = action.parser

              e.getChannel.setReadable(false)

              val eventuallyBodyParser = getBodyParser[action.BODY_CONTENT](requestHeader, bodyParser)

              val eventuallyBody =
                eventuallyBodyParser.flatMap { bodyParser =>
                  if (nettyHttpRequest.isChunked) {
                    val (result, handler) = newRequestBodyHandler(bodyParser)
                    val p: ChannelPipeline = ctx.getChannel().getPipeline()
                    p.replace("handler", "handler", handler)
                    e.getChannel.setReadable(true)

                    result
                  } else {
                    e.getChannel.setReadable(true)
                    lazy val bodyEnumerator = {
                      val body = {
                        val cBuffer = nettyHttpRequest.getContent()
                        val bytes = new Array[Byte](cBuffer.readableBytes())
                        cBuffer.readBytes(bytes)
                        bytes
                      }
                      Enumerator(body).andThen(Enumerator.enumInput(EOF))
                    }

                    (bodyParser <<: bodyEnumerator).map(p => Right(p)): Promise[Either[String, Iteratee[Array[Byte], Either[Result, action.BODY_CONTENT]]]]
                  }
                }

              val eventuallyRequest =
                eventuallyBody.map { errOrBody =>
                  errOrBody.right.map((it: Iteratee[Array[Byte], Either[Result, action.BODY_CONTENT]]) => it.run.map { (something: Either[Result, action.BODY_CONTENT]) =>

                    something match {

                      case Left(result) => Left(result)

                      case Right(b: action.BODY_CONTENT) => {
                        Right(
                          new Request[action.BODY_CONTENT] {
                            def uri = nettyHttpRequest.getUri
                            def path = nettyUri.getPath
                            def method = nettyHttpRequest.getMethod.getName
                            def queryString = parameters
                            def headers = rHeaders
                            def cookies = rCookies
                            def username = None
                            val body = b
                          })
                      }

                    }

                  })
                }

              eventuallyRequest.map {
                case Left(errMsg) =>
                  response.handle(Results.InternalServerError)
                  Promise.pure(None: Option[Request[action.BODY_CONTENT]])
                case Right(eventuallyReq) =>
                  eventuallyReq.extend(_.value match {
                    case Redeemed(Left(result)) => response.handle(result)
                    case Redeemed(Right(request)) =>
                      invoke(request, response, action.asInstanceOf[Action[action.BODY_CONTENT]], app)
                  })

              }
            }

            case Right((ws @ WebSocket(f), app)) if (isWebSocket(nettyHttpRequest)) => {
              try {
                val enumerator = websocketHandshake(ctx, nettyHttpRequest, e)
                f(requestHeader)(enumerator, socketOut(ctx)(ws.writeable))
              } catch {
                case e => e.printStackTrace
              }
            }

            case Right((WebSocket(_), _)) => {
              response.handle(Results.BadRequest)
            }

            case Left(e) => response.handle(e)

          }

        case unexpected => Logger("play").error("Oops, unexpected message received in NettyServer (please report this problem): " + unexpected)

      }
    }

  }

  class DefaultPipelineFactory extends ChannelPipelineFactory {
    def getPipeline = {
      val newPipeline = pipeline()
      newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))
      newPipeline.addLast("encoder", new HttpResponseEncoder())
      newPipeline.addLast("chunkedWriter", new ChunkedWriteHandler())
      newPipeline.addLast("handler", new PlayDefaultUpstreamHandler())
      newPipeline
    }
  }

  bootstrap.setPipelineFactory(new DefaultPipelineFactory)

  allChannels.add(bootstrap.bind(new java.net.InetSocketAddress(host, port)))

  Logger("play").info("Listening for HTTP on %s:%s...".format(host, port))

  def stop() {
    Play.stop()
    Logger("play").warn("Stopping server...")
    allChannels.disconnect().awaitUninterruptibly()
    allChannels.close().awaitUninterruptibly()
    bootstrap.releaseExternalResources()
  }

}

object NettyServer {

  import java.io._

  def createServer(applicationPath: File): Option[NettyServer] = {

    // Manage RUNNING_PID file
    java.lang.management.ManagementFactory.getRuntimeMXBean.getName.split('@').headOption.map { pid =>
      val pidFile = new File(applicationPath, "RUNNING_PID")

      if (pidFile.exists) {
        println("This application is already running (Or delete the RUNNING_PID file).")
        System.exit(-1)
      }

      println("Process ID is " + pid)

      new FileOutputStream(pidFile).write(pid.getBytes)
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run {
          pidFile.delete()
        }
      })
    }

    try {
      Some(new NettyServer(
        new StaticApplication(applicationPath),
        Option(System.getProperty("http.host")).getOrElse("0.0.0.0"),
        Option(System.getProperty("http.port")).map(Integer.parseInt(_)).getOrElse(9000)))
    } catch {
      case e => {
        println("Oops, cannot start the server.")
        e.printStackTrace()
        None
      }
    }

  }

  def main(args: Array[String]) {

    args.headOption.orElse(
      Option(System.getProperty("user.dir"))).map(new File(_)).filter(p => p.exists && p.isDirectory).map { applicationPath =>
        createServer(applicationPath).getOrElse(System.exit(-1))
      }.getOrElse {
        println("Not a valid Play application")
      }

  }

}
