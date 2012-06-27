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
import org.jboss.netty.channel.group._
import org.jboss.netty.handler.ssl._

import java.security._
import javax.net.ssl._
import java.util.concurrent._

import play.core._
import play.core.server.websocket._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.concurrent._
import play.core.server.netty._

import scala.collection.JavaConverters._

/**
 * provides a stopable Server
 */
trait ServerWithStop {
  def stop(): Unit
}

/**
 * creates a Server implementation based Netty
 */
class NettyServer(appProvider: ApplicationProvider, port: Int, sslPort: Option[Int] = None, address: String = "0.0.0.0", val mode: Mode.Mode = Mode.Prod) extends Server with ServerWithStop {

  def applicationProvider = appProvider

  private def newBootstrap = new ServerBootstrap(
    new org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()))

  class PlayPipelineFactory(secure: Boolean = false) extends ChannelPipelineFactory {
    def getPipeline = {
      val newPipeline = pipeline()

      if (secure) {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FakeKeyStore.asInputStream, FakeKeyStore.getKeyStorePassword)
        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(keyStore, FakeKeyStore.getCertificatePassword)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.getKeyManagers, null, null)
        val sslEngine = sslContext.createSSLEngine
        sslEngine.setUseClientMode(false)
        newPipeline.addLast("ssl", new SslHandler(sslEngine))
      }

      newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))
      newPipeline.addLast("encoder", new HttpResponseEncoder())
      newPipeline.addLast("compressor", new HttpContentCompressor())
      newPipeline.addLast("decompressor", new HttpContentDecompressor())	  
      newPipeline.addLast("handler", defaultUpStreamHandler)
      newPipeline
    }
  }

  // Keep a reference on all opened channels (useful to close everything properly, especially in DEV mode)
  val allChannels = new DefaultChannelGroup

  // Our upStream handler is stateless. Let's use this instance for every new connection
  val defaultUpStreamHandler = new PlayDefaultUpstreamHandler(this, allChannels)

  // The HTTP server channel
  val HTTP: Bootstrap = {
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory)
    allChannels.add(bootstrap.bind(new java.net.InetSocketAddress(address, port)))
    bootstrap
  }

  // Maybe the HTTPS server channel
  val HTTPS: Option[Bootstrap] = sslPort.map { port =>
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory(secure = true))
    allChannels.add(bootstrap.bind(new java.net.InetSocketAddress(address, port)))
    bootstrap
  }

  mode match {
    case Mode.Test =>
    case _ => {
      Logger("play").info("Listening for HTTP on port %s...".format(port))
      sslPort.foreach { port =>
        Logger("play").info("Listening for HTTPS on port %s...".format(port))
      }
    }
  }

  override def stop() {

    try {
      Play.stop()
    } catch {
      case e => Logger("play").error("Error while stopping the application", e)
    }

    try {
      super.stop()
    } catch {
      case e => Logger("play").error("Error while stopping akka", e)
    }

    mode match {
      case Mode.Test =>
      case _ => Logger("play").info("Stopping server...")
    }

    // First, close all opened sockets
    allChannels.close().awaitUninterruptibly()

    // Release the HTTP server
    HTTP.releaseExternalResources()

    // Release the HTTPS server if needed
    HTTPS.foreach(_.releaseExternalResources())

  }

}

/**
 * bootstraps Play application with a NettyServer backened
 */
object NettyServer {

  import java.io._
  import java.net._

  /**
   * creates a NettyServer based on the application represented by applicationPath
   * @param applicationPath path to application
   */
  def createServer(applicationPath: File): Option[NettyServer] = {
    // Manage RUNNING_PID file
    java.lang.management.ManagementFactory.getRuntimeMXBean.getName.split('@').headOption.map { pid =>
      val pidFile = Option(System.getProperty("pidfile.path")).map(new File(_)).getOrElse(new File(applicationPath.getAbsolutePath, "RUNNING_PID"))

      // The Logger is not initialized yet, we print the Process ID on STDOUT
      println("Play server process ID is " + pid)

      if (pidFile.getAbsolutePath != "/dev/null") {
        if (pidFile.exists) {
          println("This application is already running (Or delete "+ pidFile.getAbsolutePath +" file).")
          System.exit(-1)
        }

        new FileOutputStream(pidFile).write(pid.getBytes)
        Runtime.getRuntime.addShutdownHook(new Thread {
          override def run {
            pidFile.delete()
          }
        })
      }
    }

    try {
      val server = new NettyServer(
        new StaticApplication(applicationPath),
        Option(System.getProperty("http.port")).map(Integer.parseInt(_)).getOrElse(9000),
        Option(System.getProperty("https.port")).map(Integer.parseInt(_)),
        Option(System.getProperty("http.address")).getOrElse("0.0.0.0"))
        
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run {
          server.stop()
        }
      })
      
      Some(server)
    } catch {
      case e => {
        println("Oops, cannot start the server.")
        e.printStackTrace()
        None
      }
    }

  }

  /**
   * attempts to create a NettyServer based on either
   * passed in argument or `user.dir` System property or current directory
   * @param args
   */
  def main(args: Array[String]) {
    args.headOption.orElse(
      Option(System.getProperty("user.dir"))).map(new File(_)).filter(p => p.exists && p.isDirectory).map { applicationPath =>
        createServer(applicationPath).getOrElse(System.exit(-1))
      }.getOrElse {
        println("Not a valid Play application")
      }
  }

  /**
   * provides a NettyServer for the dev environment
   */
  def mainDev(sbtLink: SBTLink, port: Int): NettyServer = {
    play.utils.Threads.withContextClassLoader(this.getClass.getClassLoader) {
      try {
        val appProvider = new ReloadableApplication(sbtLink)
        new NettyServer(appProvider, port, mode = Mode.Dev)
      } catch {
        case e => {
          throw e match {
            case e: ExceptionInInitializerError => e.getCause
            case e => e
          }
        }
      }

    }
  }

}
