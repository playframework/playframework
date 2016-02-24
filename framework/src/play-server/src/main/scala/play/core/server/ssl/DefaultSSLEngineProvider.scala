/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.ssl

import play.core.server.ServerConfig
import play.server.api.SSLEngineProvider
import play.core.ApplicationProvider
import javax.net.ssl.{ TrustManager, KeyManagerFactory, SSLEngine, SSLContext, X509TrustManager }
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.io.{ FileInputStream, File }
import play.api.Logger
import scala.util.control.NonFatal
import play.utils.PlayIO

/**
 * This class calls sslContext.createSSLEngine() with no parameters and returns the result.
 */
class DefaultSSLEngineProvider(serverConfig: ServerConfig, appProvider: ApplicationProvider) extends SSLEngineProvider {

  import DefaultSSLEngineProvider._

  val sslContext: SSLContext = createSSLContext(appProvider)

  override def createSSLEngine: SSLEngine = {
    sslContext.createSSLEngine()
  }

  def createSSLContext(applicationProvider: ApplicationProvider): SSLContext = {
    val httpsConfig = serverConfig.configuration.underlying.getConfig("play.server.https")
    val keyStoreConfig = httpsConfig.getConfig("keyStore")
    val keyManagerFactory: KeyManagerFactory = if (keyStoreConfig.hasPath("path")) {
      val path = keyStoreConfig.getString("path")
      // Load the configured key store
      val keyStore = KeyStore.getInstance(keyStoreConfig.getString("type"))
      val password = keyStoreConfig.getString("password").toCharArray
      val algorithm = if (keyStoreConfig.hasPath("algorithm")) keyStoreConfig.getString("algorithm") else KeyManagerFactory.getDefaultAlgorithm
      val file = new File(path)
      if (file.isFile) {
        val in = new FileInputStream(file)
        try {
          keyStore.load(in, password)
          logger.debug("Using HTTPS keystore at " + file.getAbsolutePath)
          val kmf = KeyManagerFactory.getInstance(algorithm)
          kmf.init(keyStore, password)
          kmf
        } catch {
          case NonFatal(e) => {
            throw new Exception("Error loading HTTPS keystore from " + file.getAbsolutePath, e)
          }
        } finally {
          PlayIO.closeQuietly(in)
        }
      } else {
        throw new Exception("Unable to find HTTPS keystore at \"" + file.getAbsolutePath + "\"")
      }
    } else {
      // Load a generated key store
      logger.warn("Using generated key with self signed certificate for HTTPS. This should not be used in production.")
      FakeKeyStore.keyManagerFactory(serverConfig.rootDir)
    }

    // Load the configured trust manager
    val trustStoreConfig = httpsConfig.getConfig("trustStore")
    val tm = if (trustStoreConfig.getBoolean("noCaVerification")) {
      logger.warn("HTTPS configured with no client " +
        "side CA verification. Requires http://webid.info/ for client certificate verification.")
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
  val nullArray = Array[X509Certificate]()
  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}
  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}
  def getAcceptedIssuers() = nullArray
}
