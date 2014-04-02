package play.core.server

import play.server.api.SSLContextProvider
import play.core.ApplicationProvider
import javax.net.ssl.{ TrustManager, KeyManagerFactory, SSLContext }
import java.security.KeyStore
import java.io.{ FileInputStream, File }
import play.api.Play
import play.core.server.netty.FakeKeyStore
import scala.util.control.NonFatal
import scala.util.{ Try, Failure, Success }

class DefaultSSLContextProvider extends SSLContextProvider {
  override def createSSLContext(applicationProvider: ApplicationProvider): SSLContext = {
    val keyManagerFactory: Try[KeyManagerFactory] = Option(System.getProperty("https.keyStore")) match {
      case Some(path) => {
        // Load the configured key store
        val keyStore = KeyStore.getInstance(System.getProperty("https.keyStoreType", "JKS"))
        val password = System.getProperty("https.keyStorePassword", "").toCharArray
        val algorithm = System.getProperty("https.keyStoreAlgorithm", KeyManagerFactory.getDefaultAlgorithm)
        val file = new File(path)
        if (file.isFile) {
          try {
            for (in <- resource.managed(new FileInputStream(file))) {
              keyStore.load(in, password)
            }
            Play.logger.debug("Using HTTPS keystore at " + file.getAbsolutePath)
            val kmf = KeyManagerFactory.getInstance(algorithm)
            kmf.init(keyStore, password)
            Success(kmf)
          } catch {
            case NonFatal(e) => {
              Failure(new Exception("Error loading HTTPS keystore from " + file.getAbsolutePath, e))
            }
          }
        } else {
          Failure(new Exception("Unable to find HTTPS keystore at \"" + file.getAbsolutePath + "\""))
        }
      }
      case None => {
        // Load a generated key store
        Play.logger.warn("Using generated key with self signed certificate for HTTPS. This should not be used in production.")
        FakeKeyStore.keyManagerFactory(applicationProvider.path)
      }
    }

    keyManagerFactory.map { kmf =>
      // Load the configured trust manager
      val tm = Option(System.getProperty("https.trustStore")).map {
        case "noCA" => {
          Play.logger.warn("HTTPS configured with no client " +
            "side CA verification. Requires http://webid.info/ for client certificate verification.")
          Array[TrustManager](noCATrustManager)
        }
        case _ => {
          Play.logger.debug("Using default trust store for client side CA verification")
          null
        }
      }.getOrElse {
        Play.logger.debug("Using default trust store for client side CA verification")
        null
      }

      // Configure the SSL context
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, tm, null)
      sslContext
    }.get
  }
}
