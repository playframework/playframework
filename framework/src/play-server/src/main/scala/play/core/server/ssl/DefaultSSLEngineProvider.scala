package play.core.server.ssl

import play.server.api.SSLEngineProvider
import play.core.ApplicationProvider
import javax.net.ssl.{ TrustManager, KeyManagerFactory, SSLEngine, SSLContext, X509TrustManager }
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.io.{ FileInputStream, File }
import play.api.Logger
import scala.util.control.NonFatal
import scala.util.{ Try, Failure, Success }
import play.utils.PlayIO

/**
 * This class calls sslContext.createSSLEngine() with no parameters and returns the result.
 */
class DefaultSSLEngineProvider(appProvider: ApplicationProvider) extends SSLEngineProvider {

  import DefaultSSLEngineProvider._

  val sslContext: SSLContext = createSSLContext(appProvider)

  override def createSSLEngine: SSLEngine = {
    sslContext.createSSLEngine()
  }

  def createSSLContext(applicationProvider: ApplicationProvider): SSLContext = {
    val keyManagerFactory: Try[KeyManagerFactory] = Option(System.getProperty("https.keyStore")) match {
      case Some(path) => {
        // Load the configured key store
        val keyStore = KeyStore.getInstance(System.getProperty("https.keyStoreType", "JKS"))
        val password = System.getProperty("https.keyStorePassword", "").toCharArray
        val algorithm = System.getProperty("https.keyStoreAlgorithm", KeyManagerFactory.getDefaultAlgorithm)
        val file = new File(path)
        if (file.isFile) {
          val in = new FileInputStream(file)
          try {
            keyStore.load(in, password)
            logger.debug("Using HTTPS keystore at " + file.getAbsolutePath)
            val kmf = KeyManagerFactory.getInstance(algorithm)
            kmf.init(keyStore, password)
            Success(kmf)
          } catch {
            case NonFatal(e) => {
              Failure(new Exception("Error loading HTTPS keystore from " + file.getAbsolutePath, e))
            }
          } finally {
            PlayIO.closeQuietly(in)
          }
        } else {
          Failure(new Exception("Unable to find HTTPS keystore at \"" + file.getAbsolutePath + "\""))
        }
      }
      case None => {
        // Load a generated key store
        logger.warn("Using generated key with self signed certificate for HTTPS. This should not be used in production.")
        FakeKeyStore.keyManagerFactory(applicationProvider.path)
      }
    }

    keyManagerFactory.map { kmf =>
      // Load the configured trust manager
      val tm = Option(System.getProperty("https.trustStore")).map {
        case "noCA" => {
          logger.warn("HTTPS configured with no client " +
            "side CA verification. Requires http://webid.info/ for client certificate verification.")
          Array[TrustManager](noCATrustManager)
        }
        case _ => {
          logger.debug("Using default trust store for client side CA verification")
          null
        }
      }.getOrElse {
        logger.debug("Using default trust store for client side CA verification")
        null
      }

      // Configure the SSL context
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, tm, null)
      sslContext
    }.get
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
