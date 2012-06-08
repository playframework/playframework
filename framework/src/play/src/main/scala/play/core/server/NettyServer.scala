package play.core.server

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.group._
import org.jboss.netty.handler.ssl._

import java.security._
import javax.net.ssl._
import java.util.concurrent._

import play.core._
import play.api._
import play.core.server.netty._

import java.security.cert.X509Certificate
import java.io.{File, FileInputStream}
import utils.IO

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
      sslContext.map{ ctxt =>
        val sslEngine = ctxt.createSSLEngine
        sslEngine.setUseClientMode(false)
        newPipeline.addLast("ssl", new SslHandler(sslEngine))
      }
      newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))
      newPipeline.addLast("encoder", new HttpResponseEncoder())
      newPipeline.addLast("handler", defaultUpStreamHandler)
      newPipeline
    }

    lazy val sslContext: Option[SSLContext] =  //the sslContext should be reused on each connection
      for (tlsPort <- sslPort;
           app <- appProvider.get.right.toOption )
      yield {
        val config = app.configuration
        val ksAttr = "https.port" + tlsPort + ".keystore"
        val keyStore = KeyStore.getInstance(config.getString(ksAttr + ".type").getOrElse("JKS"))
        val kmfOpt: Option[Option[KeyManagerFactory]] = for (
          path <- config.getString(ksAttr + ".location");
          alias <- config.getString(ksAttr + ".alias");
          password <- config.getString(ksAttr + ".password").orElse(Some("")).map(_.toCharArray);
          algorithm <- config.getString(ksAttr + ".algorithm").orElse(Option(KeyManagerFactory.getDefaultAlgorithm))
        ) yield {
          //Logger("play").info("path="+path+" alias="+alias+" password="+password+" alg="+algorithm)
          val file = new File(path)
          if (file.isFile) {
            IO.use(new FileInputStream(file)) {
              in =>
                keyStore.load(in, password)
            }
            Logger("play").info("for port " + tlsPort + " using keystore at " + file)
            val kmf = KeyManagerFactory.getInstance(algorithm)
            kmf.init(keyStore, password) //there should be a certificate keystore
            Some(kmf)
          } else None
        }
        val kmf = kmfOpt.flatMap(a => a).orElse {
          Logger("play").warn("using localhost fake keystore for ssl connection on port " + sslPort.get)
          keyStore.load(FakeKeyStore.asInputStream, FakeKeyStore.getKeyStorePassword)
          val kmf = KeyManagerFactory.getInstance("SunX509")
          kmf.init(keyStore, FakeKeyStore.getCertificatePassword)
          Some(kmf)
        }.get

        val sslContext = SSLContext.getInstance("TLS")
        val tm = config.getString(ksAttr + ".trust").map {
          case "noCA" => {
            Logger("play").warn("secure http server (https) on port " + sslPort.get + " with no client " +
              "side CA verification. Requires http://webid.info/ for client certifiate verification.")
            Array[TrustManager](noCATrustManager)
          }
          case path => {
            Logger("play").info("no trust info for port " + tlsPort)
            null
          } //for the moment
        }.getOrElse {
          Logger("play").info("no trust attribute for port " + tlsPort)
          null
        }
        sslContext.init(kmf.getKeyManagers, tm, new SecureRandom())
        sslContext
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
      val pidPath = Option(System.getProperty("pidfile.path")).getOrElse(applicationPath.getAbsolutePath())
      val pidFile = new File(pidPath, "RUNNING_PID")

      if (pidFile.exists) {
        println("This application is already running (Or delete the RUNNING_PID file).")
        System.exit(-1)
      }

      // The Logger is not initialized yet, we print the Process ID on STDOUT
      println("Play server process ID is " + pid)

      new FileOutputStream(pidFile).write(pid.getBytes)
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run {
          pidFile.delete()
        }
      })
    }

    try {
      val app = new StaticApplication(applicationPath)
      val config = app.application.configuration
      val server = new NettyServer(
        app,
        config.getInt("http.port").getOrElse(9000),
        config.getInt("https.port"),
        config.getString("http.address").getOrElse("0.0.0.0"))
        
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
        new NettyServer(appProvider, port,
          Option(System.getProperty("https.port")).map(Integer.parseInt(_)),
          mode = Mode.Dev)
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
