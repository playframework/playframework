/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.InetSocketAddress

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.config.{ Config, ConfigValue }
import com.typesafe.netty.HandlerPublisher
import com.typesafe.netty.http.HttpStreamsServerHandler
import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.epoll.{ EpollEventLoopGroup, EpollServerSocketChannel }
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.logging.{ LogLevel, LoggingHandler }
import io.netty.handler.ssl.SslHandler
import io.netty.handler.timeout.IdleStateHandler
import play.api._
import play.api.internal.libs.concurrent.CoordinatedShutdownSupport
import play.api.routing.Router
import play.core._
import play.core.server.Server.ServerStoppedReason
import play.core.server.netty._
import play.core.server.ssl.ServerSSLEngine
import play.server.SSLEngineProvider

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

sealed trait NettyTransport
case object Jdk extends NettyTransport
case object Native extends NettyTransport

/**
 * creates a Server implementation based Netty
 */
class NettyServer(
    config: ServerConfig,
    val applicationProvider: ApplicationProvider,
    stopHook: () => Future[_],
    val actorSystem: ActorSystem)(implicit val materializer: Materializer) extends Server {

  registerShutdownTasks()

  private val serverConfig = config.configuration.get[Configuration]("play.server")
  private val nettyConfig = serverConfig.get[Configuration]("netty")
  private val serverHeader = nettyConfig.get[Option[String]]("server-header").collect { case s if s.nonEmpty => s }
  private val maxInitialLineLength = nettyConfig.get[Int]("maxInitialLineLength")
  private val maxHeaderSize = nettyConfig.get[Int]("maxHeaderSize")
  private val maxChunkSize = nettyConfig.get[Int]("maxChunkSize")
  private val logWire = nettyConfig.get[Boolean]("log.wire")

  private lazy val transport = nettyConfig.get[String]("transport") match {
    case "native" => Native
    case "jdk" => Jdk
    case _ => throw ServerStartException("Netty transport configuration value should be either jdk or native")
  }

  import NettyServer._

  override def mode: Mode = config.mode

  /**
   * The event loop
   */
  private val eventLoop = {
    val threadCount = nettyConfig.get[Int]("eventLoopThreads")
    val threadFactory = NamedThreadFactory("netty-event-loop")
    transport match {
      case Native => new EpollEventLoopGroup(threadCount, threadFactory)
      case Jdk => new NioEventLoopGroup(threadCount, threadFactory)
    }
  }

  /**
   * A reference to every channel, both server and incoming, this allows us to shutdown cleanly.
   */
  private val allChannels = new DefaultChannelGroup(eventLoop.next())

  /**
   * SSL engine provider, only created if needed.
   */
  private lazy val sslEngineProvider: Option[SSLEngineProvider] =
    try {
      Some(ServerSSLEngine.createSSLEngineProvider(config, applicationProvider))
    } catch {
      case NonFatal(e) =>
        logger.error(s"cannot load SSL context", e)
        None
    }

  private def setOptions(setOption: (ChannelOption[AnyRef], AnyRef) => Any, config: Config) = {
    def unwrap(value: ConfigValue) = value.unwrapped() match {
      case number: Number => number.intValue().asInstanceOf[Integer]
      case other => other
    }
    config.entrySet().asScala.filterNot(_.getKey.startsWith("child.")).foreach { option =>
      if (ChannelOption.exists(option.getKey)) {
        setOption(ChannelOption.valueOf(option.getKey), unwrap(option.getValue))
      } else {
        logger.warn("Ignoring unknown Netty channel option: " + option.getKey)
        transport match {
          case Native => logger.warn("Valid values can be found at http://netty.io/4.0/api/io/netty/channel/ChannelOption.html and http://netty.io/4.0/api/io/netty/channel/epoll/EpollChannelOption.html")
          case Jdk => logger.warn("Valid values can be found at http://netty.io/4.0/api/io/netty/channel/ChannelOption.html")
        }
      }
    }
  }

  /**
   * Bind to the given address, returning the server channel, and a stream of incoming connection channels.
   */
  private def bind(address: InetSocketAddress): (Channel, Source[Channel, _]) = {
    val serverChannelEventLoop = eventLoop.next

    // Watches for channel events, and pushes them through a reactive streams publisher.
    val channelPublisher = new HandlerPublisher(serverChannelEventLoop, classOf[Channel])

    val channelClass = transport match {
      case Native => classOf[EpollServerSocketChannel]
      case Jdk => classOf[NioServerSocketChannel]
    }

    val bootstrap = new Bootstrap()
      .channel(channelClass)
      .group(serverChannelEventLoop)
      .option(ChannelOption.AUTO_READ, java.lang.Boolean.FALSE) // publisher does ctx.read()
      .handler(channelPublisher)
      .localAddress(address)

    setOptions(bootstrap.option, nettyConfig.get[Config]("option"))

    val channel = bootstrap.bind.await().channel()
    allChannels.add(channel)

    (channel, Source.fromPublisher(channelPublisher))
  }

  /**
   * Create a new PlayRequestHandler.
   */
  protected[this] def newRequestHandler(): ChannelInboundHandler = new PlayRequestHandler(this, serverHeader)

  /**
   * Create a sink for the incoming connection channels.
   */
  private def channelSink(port: Int, secure: Boolean): Sink[Channel, Future[Done]] = {
    Sink.foreach[Channel] { (connChannel: Channel) =>

      // Setup the channel for explicit reads
      connChannel.config().setOption(ChannelOption.AUTO_READ, java.lang.Boolean.FALSE)

      setOptions(connChannel.config().setOption, nettyConfig.get[Config]("option.child"))

      val pipeline = connChannel.pipeline()
      if (secure) {
        sslEngineProvider.map { sslEngineProvider =>
          val sslEngine = sslEngineProvider.createSSLEngine()
          sslEngine.setUseClientMode(false)
          if (serverConfig.get[Boolean]("https.wantClientAuth")) {
            sslEngine.setWantClientAuth(true)
          }
          if (serverConfig.get[Boolean]("https.needClientAuth")) {
            sslEngine.setNeedClientAuth(true)
          }
          pipeline.addLast("ssl", new SslHandler(sslEngine))
        }
      }

      // Netty HTTP decoders/encoders/etc
      pipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize))
      pipeline.addLast("encoder", new HttpResponseEncoder())
      pipeline.addLast("decompressor", new HttpContentDecompressor())
      if (logWire) {
        pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG))
      }

      val idleTimeout = serverConfig.get[Duration](if (secure) "https.idleTimeout" else "http.idleTimeout")
      idleTimeout match {
        case Duration.Inf => // Do nothing, in other words, don't set any timeout.
        case Duration(timeout, timeUnit) =>
          logger.trace(s"using idle timeout of $timeout $timeUnit on port $port")
          // only timeout if both reader and writer have been idle for the specified time
          pipeline.addLast("idle-handler", new IdleStateHandler(0, 0, timeout, timeUnit))
      }

      val requestHandler = newRequestHandler()

      // Use the streams handler to close off the connection.
      pipeline.addLast("http-handler", new HttpStreamsServerHandler(Seq[ChannelHandler](requestHandler).asJava))

      pipeline.addLast("request-handler", requestHandler)

      // And finally, register the channel with the event loop
      val childChannelEventLoop = eventLoop.next()
      childChannelEventLoop.register(connChannel)
      allChannels.add(connChannel)
    }
  }

  // Maybe the HTTP server channel
  private val httpChannel = config.port.map(bindChannel(_, secure = false))

  // Maybe the HTTPS server channel
  private val httpsChannel = config.sslPort.map(bindChannel(_, secure = true))

  private def bindChannel(port: Int, secure: Boolean): Channel = {
    val protocolName = if (secure) "HTTPS" else "HTTP"
    val address = new InetSocketAddress(config.address, port)
    val (serverChannel, channelSource) = bind(address)
    channelSource.runWith(channelSink(port = port, secure = secure))
    val boundAddress = serverChannel.localAddress()
    if (boundAddress == null) {
      val e = new ServerListenException(protocolName, address)
      logger.error(e.getMessage)
      throw e
    }
    if (mode != Mode.Test) {
      logger.info(s"Listening for $protocolName on $boundAddress")
    }
    serverChannel
  }

  override def stop(): Unit = CoordinatedShutdownSupport.syncShutdown(actorSystem, ServerStoppedReason)

  // Using CoordinatedShutdown means that instead of invoking code imperatively in `stop`
  // we have to register it as early as possible as CoordinatedShutdown tasks and
  // then `stop` runs CoordinatedShutdown.
  private def registerShutdownTasks(): Unit = {

    implicit val ctx: ExecutionContext = actorSystem.dispatcher

    val cs = CoordinatedShutdown(actorSystem)
    cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "trace-server-stop-request") {
      () =>
        mode match {
          case Mode.Test =>
          case _ => logger.info("Stopping server...")
        }
        Future.successful(Done)
    }

    val unbindTimeout = cs.timeout(CoordinatedShutdown.PhaseServiceUnbind)
    cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "netty-server-unbind") {
      () =>
        // First, close all opened sockets
        allChannels.close().awaitUninterruptibly(unbindTimeout.toMillis - 100)
        // Now shutdown the event loop
        eventLoop.shutdownGracefully().await(unbindTimeout.toMillis - 100)
        Future.successful(Done)
    }

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "user-provided-server-stop-hook") {
      () => stopHook().map(_ => Done)
    }
    cs.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "shutdown-logger") {
      () =>
        Future {
          super.stop()
          Done
        }
    }

  }

  override lazy val mainAddress: InetSocketAddress = {
    (httpChannel orElse httpsChannel).get.localAddress().asInstanceOf[InetSocketAddress]
  }

  override def httpPort: Option[Int] = httpChannel map (_.localAddress().asInstanceOf[InetSocketAddress].getPort)

  override def httpsPort: Option[Int] = httpsChannel map (_.localAddress().asInstanceOf[InetSocketAddress].getPort)
}

/**
 * The Netty server provider
 */
class NettyServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context) = new NettyServer(
    context.config,
    context.appProvider,
    context.stopHook,
    context.actorSystem
  )(
    context.materializer
  )
}

/**
 * Create a Netty server zfrom a given router using [[BuiltInComponents]]:
 *
 * {{{
 *   val server = NettyServer.fromRouterWithComponents(ServerConfig(port = Some(9002))) { components =>
 *     import play.api.mvc.Results._
 *     import components.{ defaultActionBuilder => Action }
 *     {
 *       case GET(p"/") => Action {
 *         Ok("Hello")
 *       }
 *     }
 *   }
 * }}}
 *
 * Use this together with <a href="https://www.playframework.com/documentation/latest/ScalaSirdRouter">Sird Router</a>.
 */
object NettyServer extends ServerFromRouter {

  private val logger = Logger(this.getClass)

  implicit val provider = new NettyServerProvider

  def main(args: Array[String]): Unit = {
    System.err.println(s"NettyServer.main is deprecated. Please start your Play server with the ${ProdServerStart.getClass.getName}.main.")
    ProdServerStart.main(args)
  }

  /**
   * Create a Netty server from the given application and server configuration.
   *
   * @param application The application.
   * @param config The server configuration.
   * @return A started Netty server, serving the application.
   */
  def fromApplication(application: Application, config: ServerConfig = ServerConfig()): NettyServer = {
    new NettyServer(config, ApplicationProvider(application), () => Future.successful(()), application.actorSystem)(
      application.materializer)
  }

  override protected def createServerFromRouter(serverConf: ServerConfig)(routes: ServerComponents with BuiltInComponents => Router): Server = {
    new NettyServerComponents with BuiltInComponents with NoHttpFiltersComponents {
      override lazy val serverConfig: ServerConfig = serverConf
      override def router: Router = routes(this)
    }.server
  }
}

/**
 * Cake for building a simple Netty server.
 */
trait NettyServerComponents extends ServerComponents {
  lazy val server: NettyServer = {
    // Start the application first
    Play.start(application)
    new NettyServer(serverConfig, ApplicationProvider(application), serverStopHook, application.actorSystem)(
      application.materializer)
  }

  def application: Application
}

/**
 * A convenient helper trait for constructing an NettyServer, for example:
 *
 * {{{
 *   val components = new DefaultNettyServerComponents {
 *     override lazy val router = {
 *       case GET(p"/") => Action(parse.json) { body =>
 *         Ok("Hello")
 *       }
 *     }
 *   }
 *   val server = components.server
 * }}}
 */
trait DefaultNettyServerComponents
  extends NettyServerComponents with BuiltInComponents with NoHttpFiltersComponents
