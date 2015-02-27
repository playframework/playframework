/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import com.typesafe.config.ConfigFactory
import com.typesafe.netty.http.pipelining.HttpPipeliningHandler
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.channel.group._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.logging.LoggingHandler
import org.jboss.netty.handler.ssl._
import org.jboss.netty.logging.InternalLogLevel
import play.api._
import play.api.mvc.{ RequestHeader, Handler }
import play.api.routing.Router
import play.core._
import play.core.server.netty._
import play.core.server.ssl.ServerSSLEngine
import play.server.SSLEngineProvider
import scala.util.Success
import scala.util.control.NonFatal

/**
 * creates a Server implementation based Netty
 */
class NettyServer(config: ServerConfig, appProvider: ApplicationProvider) extends Server {

  private val nettyConfig = config.configuration.underlying.getConfig("play.server.netty")

  import NettyServer._

  def applicationProvider = appProvider
  def mode = config.mode

  private def newBootstrap: ServerBootstrap = {
    val serverBootstrap = new ServerBootstrap(
      new org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(NamedThreadFactory("netty-boss")),
        Executors.newCachedThreadPool(NamedThreadFactory("netty-worker"))))

    import scala.collection.JavaConversions._
    // Find all properties that start with http.netty.option

    val options = nettyConfig.getConfig("option")

    object ExtractInt {
      def unapply(s: String) = try {
        Some(s.toInt)
      } catch {
        case e: NumberFormatException => None
      }
    }

    options.entrySet().foreach { entry =>
      val value = entry.getValue.unwrapped() match {
        case bool: java.lang.Boolean => bool
        case number: Number => number
        case null => null
        case "true" | "yes" => true
        case "false" | "no" => false
        case ExtractInt(number) => number
        case string: String => string
        case other => other.toString
      }

      val name = entry.getKey

      serverBootstrap.setOption(name, value)
    }

    serverBootstrap
  }

  private class PlayPipelineFactory(secure: Boolean = false) extends ChannelPipelineFactory {

    private val logger = Logger(classOf[PlayPipelineFactory])

    def getPipeline = {
      val newPipeline = pipeline()
      if (secure) {
        sslEngineProvider.map { sslEngineProvider =>
          val sslEngine = sslEngineProvider.createSSLEngine()
          sslEngine.setUseClientMode(false)
          newPipeline.addLast("ssl", new SslHandler(sslEngine))
        }
      }
      val maxInitialLineLength = nettyConfig.getInt("maxInitialLineLength")
      val maxHeaderSize = nettyConfig.getInt("maxHeaderSize")
      val maxChunkSize = nettyConfig.getInt("maxChunkSize")
      newPipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize))
      newPipeline.addLast("encoder", new HttpResponseEncoder())
      newPipeline.addLast("decompressor", new HttpContentDecompressor())
      val logWire = nettyConfig.getBoolean("log.wire")
      if (logWire) {
        newPipeline.addLast("logging", new LoggingHandler(InternalLogLevel.DEBUG))
      }
      newPipeline.addLast("http-pipelining", new HttpPipeliningHandler())
      newPipeline.addLast("handler", defaultUpStreamHandler)
      newPipeline
    }

    lazy val sslEngineProvider: Option[SSLEngineProvider] = //the sslContext should be reused on each connection
      try {
        Some(ServerSSLEngine.createSSLEngineProvider(config, applicationProvider))
      } catch {
        case NonFatal(e) =>
          logger.error(s"cannot load SSL context", e)
          None
      }

  }

  // Keep a reference on all opened channels (useful to close everything properly, especially in DEV mode)
  private val allChannels = new DefaultChannelGroup

  // Our upStream handler is stateless. Let's use this instance for every new connection
  private val defaultUpStreamHandler = new PlayDefaultUpstreamHandler(this, allChannels)

  // The HTTP server channel
  private val httpChannel = config.port.map { port =>
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory)
    val channel = bootstrap.bind(new InetSocketAddress(config.address, port))
    allChannels.add(channel)
    (bootstrap, channel)
  }

  // Maybe the HTTPS server channel
  private val httpsChannel = config.sslPort.map { port =>
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory(secure = true))
    val channel = bootstrap.bind(new InetSocketAddress(config.address, port))
    allChannels.add(channel)
    (bootstrap, channel)
  }

  mode match {
    case Mode.Test =>
    case _ =>
      httpChannel.foreach { http =>
        logger.info("Listening for HTTP on %s".format(http._2.getLocalAddress))
      }
      httpsChannel.foreach { https =>
        logger.info("Listening for HTTPS on port %s".format(https._2.getLocalAddress))
      }
  }

  override def stop() {

    appProvider.current.foreach(Play.stop)

    try {
      super.stop()
    } catch {
      case NonFatal(e) => logger.error("Error while stopping logger", e)
    }

    mode match {
      case Mode.Test =>
      case _ => logger.info("Stopping server...")
    }

    // First, close all opened sockets
    allChannels.close().awaitUninterruptibly()

    // Release the HTTP server
    httpChannel.foreach(_._1.releaseExternalResources())

    // Release the HTTPS server if needed
    httpsChannel.foreach(_._1.releaseExternalResources())

    mode match {
      case Mode.Dev =>
        Invoker.lazySystem.close()
        Execution.lazyContext.close()
      case _ => ()
    }
  }

  override lazy val mainAddress = {
    (httpChannel orElse httpsChannel).get._2.getLocalAddress.asInstanceOf[InetSocketAddress]
  }

  def httpPort = httpChannel map (_._2.getLocalAddress.asInstanceOf[InetSocketAddress].getPort)

  def httpsPort = httpsChannel map (_._2.getLocalAddress.asInstanceOf[InetSocketAddress].getPort)
}

/**
 * The Netty server provider
 */
class NettyServerProvider extends ServerProvider {
  def createServer(config: ServerConfig, appProvider: ApplicationProvider) = new NettyServer(config, appProvider)
}

/**
 * Bootstraps Play application with a NettyServer backend.
 */
object NettyServer extends ServerStart {

  private val logger = Logger(this.getClass)

  val defaultServerProvider = new NettyServerProvider

  implicit val provider = defaultServerProvider

  /**
   * Create a Netty server from the given application and server configuration.
   *
   * @param application The application.
   * @param config The server configuration.
   * @return A started Netty server, serving the application.
   */
  def fromApplication(application: Application, config: ServerConfig = ServerConfig()): NettyServer = {
    new NettyServer(config, new ApplicationProvider {
      def get = Success(application)
      def path = config.rootDir
    })
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
    NettyServer.fromApplication(application, serverConfig)
  }

  lazy val environment: Environment = Environment.simple(mode = serverConfig.mode)
  lazy val sourceMapper: Option[SourceMapper] = None
  lazy val webCommands: WebCommands = new DefaultWebCommands
  lazy val configuration: Configuration = Configuration(ConfigFactory.load())

  def application: Application
}

