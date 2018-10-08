/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import java.security.KeyStore
import javax.net.ssl._

import com.typesafe.sslconfig.ssl.{ FakeKeyStore, FakeSSLTools }

import play.api.test.PlayRunners
import play.api.{ Application, Mode }
import play.api.test.ApplicationFactory
import play.core.ApplicationProvider
import play.core.server.ServerConfig
import play.it.test.ServerEndpoint.ClientSsl
import play.server.api.SSLEngineProvider

import scala.util.control.NonFatal

/**
 * Contains information about which port and protocol can be used to connect to the server.
 * This class is used to abstract out the details of connecting to different backends
 * and protocols. Most tests will operate the same no matter which endpoint they
 * are connected to.
 */
final case class ServerEndpoint(
    description: String,
    scheme: String,
    host: String,
    port: Int,
    expectedHttpVersions: Set[String],
    expectedServerAttr: Option[String],
    ssl: Option[ClientSsl]
) {

  /**
   * Create a full URL out of a path. E.g. a path of `/foo` becomes `http://localhost:12345/foo`
   */
  def pathUrl(path: String): String = s"$scheme://$host:$port$path"

}

object ServerEndpoint {
  /** Contains SSL information for a client that wants to connect to a [[ServerEndpoint]]. */
  final case class ClientSsl(sslContext: SSLContext, trustManager: X509TrustManager)

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

      FakeSSLTools.buildContextAndTrust(keyStore)
    }
  }
}
