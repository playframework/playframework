/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ ConfigValue, Config, ConfigFactory }
import java.net.InetSocketAddress
import com.typesafe.netty.{ HandlerSubscriber, HandlerPublisher }
import com.typesafe.netty.http.HttpStreamsServerHandler
import io.netty.bootstrap.Bootstrap
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.handler.codec.http._
import io.netty.handler.logging.{ LogLevel, LoggingHandler }
import io.netty.handler.ssl.SslHandler
import play.api._
import play.api.mvc.{ RequestHeader, Handler }
import play.api.routing.Router
import play.core._
import play.core.server.netty._
import play.core.server.ssl.ServerSSLEngine
import play.server.SSLEngineProvider
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

sealed trait NettyTransport
case object Jdk extends NettyTransport
case object Native extends NettyTransport

/**
 * creates a Server implementation based Netty
 */
class NettyServer(
    config: ServerConfig,
    val applicationProvider: ApplicationProvider,
    stopHook: () => Future[Unit],
    val actorSystem: ActorSystem)(implicit val materializer: Materializer) extends Server {

  private val nettyConfig = config.configuration.underlying.getConfig("play.server.netty")
  private val maxInitialLineLength = nettyConfig.getInt("maxInitialLineLength")
  private val maxHeaderSize = nettyConfig.getInt("maxHeaderSize")
  private val maxChunkSize = nettyConfig.getInt("maxChunkSize")
  private val logWire = nettyConfig.getBoolean("log.wire")

  private lazy val transport = nettyConfig.getString("transport") match {
    case "native" => Native
    case "jdk" => Jdk
    case _ => throw ServerStartException("Netty transport configuration value should be either jdk or native")
  }

  import NettyServer._

  def mode = config.mode

  /**
   * The event loop
   */
  private val eventLoop = {
    val threadCount = nettyConfig.getInt("eventLoopThreads")
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
    config.entrySet().asScala.filterNot(_.getKey == "child").foreach { option =>
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

    setOptions(bootstrap.option, nettyConfig.getConfig("option"))

    val channel = bootstrap.bind.await().channel()
    allChannels.add(channel)

    (channel, Source(channelPublisher))
  }

  /**
   * Create a sink for the incoming connection channels.
   */
  private def channelSink(secure: Boolean): Sink[Channel, Future[Unit]] = {
    Sink.foreach[Channel] { (connChannel: Channel) =>

      // Setup the channel for explicit reads
      connChannel.config().setOption(ChannelOption.AUTO_READ, java.lang.Boolean.FALSE)

      setOptions(connChannel.config().setOption, nettyConfig.getConfig("option.child"))

      val pipeline = connChannel.pipeline()
      if (secure) {
        sslEngineProvider.map { sslEngineProvider =>
          val sslEngine = sslEngineProvider.createSSLEngine()
          sslEngine.setUseClientMode(false)
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

      val requestHandler = new PlayRequestHandler(this)

      // Use the streams handler to close off the connection.
      pipeline.addLast("http-handler", new HttpStreamsServerHandler(Seq[ChannelHandler](requestHandler).asJava))

      pipeline.addLast("request-handler", requestHandler)

      // And finally, register the channel with the event loop
      val childChannelEventLoop = eventLoop.next()
      childChannelEventLoop.register(connChannel)
      allChannels.add(connChannel)
    }
  }

  private def handleSubscriberError(error: Throwable): Unit = {
    error match {
      // IO exceptions happen all the time, it usually just means that the client has closed the connection before fully
      // sending/receiving the response.
      case e: IOException =>
        logger.trace("Benign IO exception caught in Netty", e)
      case e =>
        logger.error("Exception caught in Netty", e)
    }
  }

  // Maybe the HTTP server channel
  private val httpChannel = config.port.map { port =>
    val (serverChannel, channelSource) = bind(new InetSocketAddress(config.address, port))
    channelSource.runWith(channelSink(secure = false))
    serverChannel
  }

  // Maybe the HTTPS server channel
  private val httpsChannel = config.sslPort.map { port =>
    val (serverChannel, channelSource) = bind(new InetSocketAddress(config.address, port))
    channelSource.runWith(channelSink(secure = true))
    serverChannel
  }

  mode match {
    case Mode.Test =>
    case _ =>
      httpChannel.foreach { http =>
        logger.info(s"Listening for HTTP on ${http.localAddress()}")
      }
      httpsChannel.foreach { https =>
        logger.info(s"Listening for HTTPS on ${https.localAddress()}")
      }
  }

  override def stop() {

    // First, close all opened sockets
    allChannels.close().awaitUninterruptibly()

    // Now shutdown the event loop
    eventLoop.shutdownGracefully()

    // Now shut the application down
    applicationProvider.current.foreach(Play.stop)

    try {
      super.stop()
    } catch {
      case NonFatal(e) => logger.error("Error while stopping logger", e)
    }

    mode match {
      case Mode.Test =>
      case _ => logger.info("Stopping server...")
    }

    // Call provided hook
    // Do this last because the hooks were created before the server,
    // so the server might need them to run until the last moment.
    Await.result(stopHook(), Duration.Inf)
  }

  override lazy val mainAddress = {
    (httpChannel orElse httpsChannel).get.localAddress().asInstanceOf[InetSocketAddress]
  }

  def httpPort = httpChannel map (_.localAddress().asInstanceOf[InetSocketAddress].getPort)

  def httpsPort = httpsChannel map (_.localAddress().asInstanceOf[InetSocketAddress].getPort)
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
 * Bootstraps Play application with a NettyServer backend.
 */
object NettyServer {

  private val logger = Logger(this.getClass)

  implicit val provider = new NettyServerProvider

  def main(args: Array[String]) {
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

  /**
   * Create a Netty server from the given router and server config.
   */
  def fromRouter(config: ServerConfig = ServerConfig())(routes: PartialFunction[RequestHeader, Handler]): NettyServer = {
    new NettyServerComponents with BuiltInComponents {
      override lazy val serverConfig = config
      lazy val router = Router.from(routes)
    }.server
  }
}

/**
 * Cake for building a simple Netty server.
 */
trait NettyServerComponents {
  lazy val serverConfig: ServerConfig = ServerConfig()
  lazy val server: NettyServer = {
    // Start the application first
    Play.start(application)
    new NettyServer(serverConfig, ApplicationProvider(application), serverStopHook, application.actorSystem)(
      application.materializer)
  }

  lazy val environment: Environment = Environment.simple(mode = serverConfig.mode)
  lazy val sourceMapper: Option[SourceMapper] = None
  lazy val webCommands: WebCommands = new DefaultWebCommands
  lazy val configuration: Configuration = Configuration(ConfigFactory.load())

  def application: Application

  /**
   * Called when Server.stop is called.
   */
  def serverStopHook: () => Future[Unit] = () => Future.successful(())
}