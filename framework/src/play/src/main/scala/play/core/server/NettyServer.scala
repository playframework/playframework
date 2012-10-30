package play.core.server

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.group._
import org.jboss.netty.handler.ssl._

import java.security._
import java.net.{ InetSocketAddress }
import javax.net.ssl._
import java.util.concurrent._

import play.core._
import play.api._
import play.core.server.netty._

import java.security.cert.X509Certificate
import java.io.{File, FileInputStream}

/**
 * provides a stopable Server
 */
trait ServerWithStop {
  def stop(): Unit
  def mainAddress: InetSocketAddress
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
        sslContext.map { ctxt =>
            val sslEngine = ctxt.createSSLEngine
            sslEngine.setUseClientMode(false)
            newPipeline.addLast("ssl", new SslHandler(sslEngine))
        }
      }
      newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))
      newPipeline.addLast("encoder", new HttpResponseEncoder())
      newPipeline.addLast("compressor", new HttpContentCompressor())
      newPipeline.addLast("decompressor", new HttpContentDecompressor())
      newPipeline.addLast("handler", defaultUpStreamHandler)
      newPipeline
    }

    lazy val sslContext: Option[SSLContext] =  //the sslContext should be reused on each connection
      Option(System.getProperty("https.keyStore")) map { path =>
        // Load the configured key store
        val keyStore = KeyStore.getInstance(System.getProperty("https.keyStoreType", "JKS"))
        val password = System.getProperty("https.keyStorePassword", "").toCharArray
        val algorithm = System.getProperty("https.keyStoreAlgorithm", KeyManagerFactory.getDefaultAlgorithm)
        val file = new File(path)
        if (file.isFile) {
          for (in <- resource.managed(new FileInputStream(file))) {
              keyStore.load(in, password)
          }
          Logger("play").debug("Using HTTPS keystore at " + file.getAbsolutePath)
          try {
            val kmf = KeyManagerFactory.getInstance(algorithm)
            kmf.init(keyStore, password)
            Some(kmf)
          } catch {
            case e: Exception => {
              Logger("play").error("Error loading HTTPS keystore from " + file.getAbsolutePath, e)
              None
            }
          }
        } else {
          Logger("play").error("Unable to find HTTPS keystore at \"" + file.getAbsolutePath + "\"")
          None
        }
      } orElse {

        // Load a generated key store
        Logger("play").warn("Using generated key with self signed certificate for HTTPS. This should not be used in production.")
        Some(FakeKeyStore.keyManagerFactory(applicationProvider.path))

      } flatMap { a => a } map { kmf =>
          // Load the configured trust manager
          val tm = Option(System.getProperty("https.trustStore")).map {
            case "noCA" => {
              Logger("play").warn("HTTPS configured with no client " +
                "side CA verification. Requires http://webid.info/ for client certifiate verification.")
              Array[TrustManager](noCATrustManager)
            }
            case _ => {
              Logger("play").debug("Using default trust store for client side CA verification")
              null
            }
          }.getOrElse {
            Logger("play").debug("Using default trust store for client side CA verification")
            null
          }

          // Configure the SSL context
          val sslContext = SSLContext.getInstance("TLS")
          sslContext.init(kmf.getKeyManagers, tm, null)
          sslContext
        }
      }

  // Keep a reference on all opened channels (useful to close everything properly, especially in DEV mode)
  val allChannels = new DefaultChannelGroup

  // Our upStream handler is stateless. Let's use this instance for every new connection
  val defaultUpStreamHandler = new PlayDefaultUpstreamHandler(this, allChannels)

  // The HTTP server channel
  val HTTP = {
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory)
    val channel = bootstrap.bind(new InetSocketAddress(address, port))
    allChannels.add(channel)
    (bootstrap, channel)
  }

  // Maybe the HTTPS server channel
  val HTTPS = sslPort.map { port =>
    val bootstrap = newBootstrap
    bootstrap.setPipelineFactory(new PlayPipelineFactory(secure = true))
    val channel = bootstrap.bind(new InetSocketAddress(address, port))
    allChannels.add(channel)
    (bootstrap, channel)
  }

  mode match {
    case Mode.Test =>
    case _ => {
      Logger("play").info("Listening for HTTP on %s".format(HTTP._2.getLocalAddress))
      HTTPS.foreach { https =>
        Logger("play").info("Listening for HTTPS on port %s".format(https._2.getLocalAddress))
      }
    }
  }

  override def stop() {

    try {
      Play.stop()
    } catch {
      case e: Exception => Logger("play").error("Error while stopping the application", e)
    }

    try {
      super.stop()
    } catch {
      case e: Exception => Logger("play").error("Error while stopping akka", e)
    }

    mode match {
      case Mode.Test =>
      case _ => Logger("play").info("Stopping server...")
    }

    // First, close all opened sockets
    allChannels.close().awaitUninterruptibly()

    // Release the HTTP server
    HTTP._1.releaseExternalResources()

    // Release the HTTPS server if needed
    HTTPS.foreach(_._1.releaseExternalResources())

  }

  override lazy val mainAddress = HTTP._2.getLocalAddress.asInstanceOf[InetSocketAddress]

}

object noCATrustManager extends X509TrustManager {
  val nullArray = Array[X509Certificate]()
  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}
  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}
  def getAcceptedIssuers() = nullArray
}

/**
 * bootstraps Play application with a NettyServer backened
 */
object NettyServer {

  import java.io._

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
          println("This application is already running (Or delete " + pidFile.getAbsolutePath + " file).")
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
        Option(System.getProperty("http.address")).getOrElse("0.0.0.0")
      )

      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run {
          server.stop()
        }
      })

      Some(server)
    } catch {
      case e: Exception => {
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
        new NettyServer(appProvider, port,
          Option(System.getProperty("https.port")).map(Integer.parseInt(_)),
          mode = Mode.Dev)
      } catch {
        case e: ExceptionInInitializerError => throw e.getCause
      }

    }
  }

}
