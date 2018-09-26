/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import java.security.KeyStore
import javax.net.ssl._

import play.api.test.PlayRunners
import play.api.{ Application, Mode }
import play.core.ApplicationProvider
import play.core.server.ServerConfig
import play.core.server.ssl.FakeKeyStore
import play.it.test.HttpsEndpoint.ServerSSL
import play.server.api.SSLEngineProvider

import scala.concurrent.Await
import scala.util.control.NonFatal

/**
 * Contains information about the port and protocol used to connect to the server.
 * This class is used to abstract out the details of connecting to different backends
 * and protocols. Most tests will operate the same no matter which endpoint they
 * are connected to.
 */
sealed trait ServerEndpoint {
  def description: String
  def scheme: String
  def port: Int
  def expectedHttpVersions: Set[String]
  def expectedServerAttr: Option[String]
  final override def toString = description

  /**
   * Create a full URL out of a path. E.g. a path of `/foo` http://localhost:12345/foo`
   */
  final def pathUrl(path: String): String = s"$scheme://localhost:$port$path"
}
/** Represents an HTTP connection to a server. */
trait HttpEndpoint extends ServerEndpoint {
  override final val scheme: String = "http"
}
/** Represents an HTTPS connection to a server. */
trait HttpsEndpoint extends ServerEndpoint {
  override final val scheme: String = "https"
  /** Information about the server's SSL setup. */
  def serverSsl: ServerSSL
}

object HttpsEndpoint {
  /** Contains information how SSL is configured for an [[HttpsEndpoint]]. */
  case class ServerSSL(sslContext: SSLContext, trustManager: X509TrustManager)
}

object ServerEndpoint {
  /**
   * Starts a server by following a [[ServerEndpointRecipe]] and using the
   * application provided by an [[ApplicationFactory]]. The server's endpoint
   * is passed to the given `block` of code.
   */
  def startEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory): (ServerEndpoint, AutoCloseable) = {
    val application: Application = appFactory.create()

    // Create a ServerConfig with dynamic ports and using a self-signed certificate
    val serverConfig = {
      val sc: ServerConfig = ServerConfig(
        port = endpointRecipe.configuredHttpPort,
        sslPort = endpointRecipe.configuredHttpsPort,
        mode = Mode.Test,
        rootDir = application.path
      )
      val patch = endpointRecipe.serverConfiguration
      sc.copy(configuration = sc.configuration ++ patch)
    }

    // Initialize and start the TestServer
    val testServer: play.api.test.TestServer = new play.api.test.TestServer(
      serverConfig, application, Some(endpointRecipe.serverProvider)
    )

    val runSynchronized = application.globalApplicationEnabled
    if (runSynchronized) {
      PlayRunners.mutex.lock()
    }
    val stopEndpoint = new AutoCloseable {
      override def close(): Unit = {
        testServer.stop()
        if (runSynchronized) {
          PlayRunners.mutex.unlock()
        }
      }
    }
    try {
      testServer.start()
      val endpoint: ServerEndpoint = endpointRecipe.createEndpointFromServer(testServer)
      (endpoint, stopEndpoint)
    } catch {
      case NonFatal(e) =>
        stopEndpoint.close()
        throw e
    }
  }

  def withEndpoint[A](endpointRecipe: ServerEndpointRecipe, appFactory: ApplicationFactory)(block: ServerEndpoint => A): A = {
    val (endpoint, endpointCloseable) = startEndpoint(endpointRecipe, appFactory)
    try block(endpoint) finally endpointCloseable.close()
  }

  /**
   * An SSLEngineProvider which simply references the values in the
   * SelfSigned object.
   */
  private[test] class SelfSignedSSLEngineProvider(serverConfig: ServerConfig, appProvider: ApplicationProvider) extends SSLEngineProvider {
    override def createSSLEngine: SSLEngine = SelfSigned.sslContext.createSSLEngine()
  }

  /**
   * Contains a statically initialized self-signed certificate.
   */
  private[test] object SelfSigned {

    /**
     * The SSLContext and TrustManager associated with the self-signed certificate.
     */
    lazy val (sslContext, trustManager): (SSLContext, X509TrustManager) = {
      val keyStore: KeyStore = FakeKeyStore.generateKeyStore

      val kmf: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      kmf.init(keyStore, "".toCharArray)
      val kms: Array[KeyManager] = kmf.getKeyManagers

      val tmf: TrustManagerFactory = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
      tmf.init(keyStore)
      val tms: Array[TrustManager] = tmf.getTrustManagers
      val x509TrustManager: X509TrustManager = tms(0).asInstanceOf[X509TrustManager]

      val sslContext: SSLContext = SSLContext.getInstance("TLS")
      sslContext.init(kms, tms, null)

      (sslContext, x509TrustManager)
    }
  }
}