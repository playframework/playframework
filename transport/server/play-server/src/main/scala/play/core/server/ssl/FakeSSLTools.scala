/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import java.security.KeyStore
import javax.net.ssl._

private[server] object FakeSSLTools {

  /**
   * NOT FOR PRODUCTION USE. Builds a "TLS" `SSLContext` and `X509TrustManager` initializing both with the keys and
   * certificates in the provided `KeyStore`. This means the `SSLContext` will produce `SSLEngine`'s, `SSLSocket``s
   * and `SSLServerSocket`'s that will use a `KeyPair` from the `KeyStore`, and will trust any  `trustedCertEntry`
   * in the `KeyStore`.
   *
   * This is a naïve implementation for testing purposes only.
   */
  def buildContextAndTrust(keyStore: KeyStore): (SSLContext, X509TrustManager) = {
    val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(keyStore, Array.emptyCharArray)
    val kms: Array[KeyManager] = kmf.getKeyManagers

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(keyStore)
    val tms: Array[TrustManager] = tmf.getTrustManagers

    val x509TrustManager: X509TrustManager = tms(0).asInstanceOf[X509TrustManager]

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(kms, tms, null)

    (sslContext, x509TrustManager)
  }
}
