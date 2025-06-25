/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import java.io.File
import java.security.cert.X509Certificate
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import scala.util.control.NonFatal

import com.typesafe.sslconfig.{ ssl => sslconfig }
import com.typesafe.sslconfig.util.NoopLogger
import play.api.Logger
import play.core.server.ServerConfig
import play.core.ApplicationProvider
import play.server.api.SSLEngineProvider
import play.utils.PlayIO

/**
 * This class calls sslContext.createSSLEngine() with no parameters and returns the result.
 */
class DefaultSSLEngineProvider(serverConfig: ServerConfig, appProvider: ApplicationProvider) extends SSLEngineProvider {
  import DefaultSSLEngineProvider._

  override val sslContext: SSLContext     = createSSLContext(appProvider)
  override def createSSLEngine: SSLEngine = sslContext.createSSLEngine()

  private def createSSLContext(applicationProvider: ApplicationProvider): SSLContext = {
    val httpsConfig                          = serverConfig.configuration.underlying.getConfig("play.server.https")
    val keyStoreConfig                       = httpsConfig.getConfig("keyStore")
    val keyManagerFactory: KeyManagerFactory = if (keyStoreConfig.hasPath("path")) {
      val path = keyStoreConfig.getString("path")
      // Load the configured key store
      val keyStore  = KeyStore.getInstance(keyStoreConfig.getString("type"))
      val password  = keyStoreConfig.getString("password").toCharArray
      val algorithm =
        if (keyStoreConfig.hasPath("algorithm")) keyStoreConfig.getString("algorithm")
        else KeyManagerFactory.getDefaultAlgorithm
      val file = new File(path)
      if (file.isFile) {
        val in = java.nio.file.Files.newInputStream(file.toPath)
        try {
          keyStore.load(in, password)
          logger.debug("Using HTTPS keystore at " + file.getAbsolutePath)
          val kmf = KeyManagerFactory.getInstance(algorithm)
          kmf.init(keyStore, password)
          kmf
        } catch {
          case NonFatal(e) => throw new Exception("Error loading HTTPS keystore from " + file.getAbsolutePath, e)
        } finally {
          PlayIO.closeQuietly(in)
        }
      } else {
        throw new Exception("Unable to find HTTPS keystore at \"" + file.getAbsolutePath + "\"")
      }
    } else {
      // Load a generated key store
      logger.warn("Using generated key with self signed certificate for HTTPS. This should NOT be used in production.")
      val FakeKeyStore = new sslconfig.FakeKeyStore(NoopLogger.factory())
      FakeKeyStore.keyManagerFactory(serverConfig.rootDir)
    }

    // Load the configured trust manager
    val trustStoreConfig = httpsConfig.getConfig("trustStore")
    val tm               = if (trustStoreConfig.getBoolean("noCaVerification")) {
      logger.warn(
        "HTTPS configured with no client " +
          "side CA verification. Requires http://webid.info/ for client certificate verification."
      )
      Array[TrustManager](noCATrustManager)
    } else {
      logger.debug("Using default trust store for client side CA verification")
      null
    }

    // Configure the SSL context
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tm, null)
    sslContext
  }
}

object DefaultSSLEngineProvider {
  private val logger = Logger(classOf[DefaultSSLEngineProvider])
}

object noCATrustManager extends X509TrustManager {
  private val nullArray: Array[X509Certificate] = Array[X509Certificate]()

  override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
  override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
  override def getAcceptedIssuers(): Array[X509Certificate]                                  = nullArray
}
