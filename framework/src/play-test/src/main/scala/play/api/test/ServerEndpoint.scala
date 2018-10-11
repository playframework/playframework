/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.security.KeyStore
import javax.net.ssl._

import com.typesafe.sslconfig.ssl.{ FakeKeyStore, FakeSSLTools }

import akka.annotation.ApiMayChange

import play.api.test.ServerEndpoint.ClientSsl
import play.core.ApplicationProvider
import play.core.server.ServerConfig
import play.server.api.SSLEngineProvider

/**
 * Contains information about which port and protocol can be used to connect to the server.
 * This class is used to abstract out the details of connecting to different backends
 * and protocols. Most tests will operate the same no matter which endpoint they
 * are connected to.
 */
@ApiMayChange final case class ServerEndpoint(
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

@ApiMayChange object ServerEndpoint {
  /** Contains SSL information for a client that wants to connect to a [[ServerEndpoint]]. */
  final case class ClientSsl(sslContext: SSLContext, trustManager: X509TrustManager)

  /**
   * An SSLEngineProvider which simply references the values in the
   * SelfSigned object.
   */
  // public only for testing purposes
  final class SelfSignedSSLEngineProvider(serverConfig: ServerConfig, appProvider: ApplicationProvider) extends SSLEngineProvider {
    override def createSSLEngine: SSLEngine = SelfSigned.sslContext.createSSLEngine()
  }

  /**
   * Contains a statically initialized self-signed certificate.
   */
  // public only for testing purposes
  object SelfSigned {

    /**
     * The SSLContext and TrustManager associated with the self-signed certificate.
     */
    lazy val (sslContext, trustManager): (SSLContext, X509TrustManager) = {
      val keyStore: KeyStore = FakeKeyStore.generateKeyStore

      FakeSSLTools.buildContextAndTrust(keyStore)
    }
  }
}
