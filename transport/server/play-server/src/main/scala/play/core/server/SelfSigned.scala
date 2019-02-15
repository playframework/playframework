/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.security.KeyStore
import javax.net.ssl._

import com.typesafe.sslconfig.ssl.{ FakeKeyStore, FakeSSLTools }

import akka.annotation.ApiMayChange

import play.core.ApplicationProvider
import play.server.api.SSLEngineProvider

/** Contains a statically initialized self-signed certificate. */
// public only for testing purposes
@ApiMayChange object SelfSigned {
  /** The SSLContext and TrustManager associated with the self-signed certificate. */
  lazy val (sslContext, trustManager): (SSLContext, X509TrustManager) = {
    val keyStore: KeyStore = FakeKeyStore.generateKeyStore
    FakeSSLTools.buildContextAndTrust(keyStore)
  }
}

/** An SSLEngineProvider which simply references the values in the SelfSigned object. */
// public only for testing purposes
@ApiMayChange final class SelfSignedSSLEngineProvider(
    serverConfig: ServerConfig,
    appProvider: ApplicationProvider
) extends SSLEngineProvider {

  def createSSLEngine: SSLEngine = SelfSigned.sslContext.createSSLEngine()

}
