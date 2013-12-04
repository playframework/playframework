/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import javax.net.ssl._
import java.security.{UnrecoverableKeyException, KeyStore, SecureRandom}
import java.security.cert._
import scala.util.control.NonFatal
import java.util.Locale
import play.api.libs.ws.ssl.AlgorithmConstraintsParser._

trait SSLContextBuilder {
  def build(): SSLContext
}

/**
 * A simple SSL context builder.  If the keyManagers or trustManagers are empty, then null is used in the init method.
 * Likewise, if secureRandom is None then null is used.
 */
class SimpleSSLContextBuilder(protocol: String,
                              keyManagers: Seq[KeyManager],
                              trustManagers: Seq[TrustManager],
                              secureRandom: Option[SecureRandom]) extends SSLContextBuilder {

  def nullIfEmpty[T](array: Array[T]) = {
    if (array.isEmpty) null else array
  }

  /**
   * Builds the appropriate SSL context manager.
   * @return a configured SSL context.
   */
  def build(): SSLContext = {

    // We deliberately do not pass in a provider, on the recommendation of
    // http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider
    //
    // "REMINDER: Cryptographic implementations in the JDK are distributed through several different providers
    // ("Sun", "SunJSSE", "SunJCE", "SunRsaSign") for both historical reasons and by the types of services provided.
    // General purpose applications SHOULD NOT request cryptographic services from specific providers."

    val sslContext = SSLContext.getInstance(protocol)

    sslContext.init(nullIfEmpty(keyManagers.toArray), nullIfEmpty(trustManagers.toArray), secureRandom.orNull)
    sslContext
  }
}

// the KeyManagerFactory and TrustManagerFactory use final methods and protected abstract constructors that make
// mocking tough.  Either we provide a wrapper, or we set up our own "mock" provider, or we use Powermock.

trait KeyManagerFactoryWrapper {

  def init(keystore: KeyStore, password: Array[Char]): Unit

  def getKeyManagers: Array[KeyManager]
}

trait TrustManagerFactoryWrapper {

  def init(spec: ManagerFactoryParameters): Unit

  def getTrustManagers: Array[TrustManager]
}

class DefaultKeyManagerFactoryWrapper(keyManagerAlgorithm: String) extends KeyManagerFactoryWrapper {
  private val instance = KeyManagerFactory.getInstance(keyManagerAlgorithm)

  def init(keystore: KeyStore, password: Array[Char]) {
    instance.init(keystore, password)
  }

  def getKeyManagers: Array[KeyManager] = instance.getKeyManagers
}

class DefaultTrustManagerFactoryWrapper(trustManagerAlgorithm: String) extends TrustManagerFactoryWrapper {
  private val instance = TrustManagerFactory.getInstance(trustManagerAlgorithm)

  def init(spec: ManagerFactoryParameters) {
    instance.init(spec)
  }

  def getTrustManagers: Array[TrustManager] = instance.getTrustManagers()
}

/**
 * Creates an SSL context builder from info objects.
 */
class ConfigSSLContextBuilder(info: SSLConfig,
                              keyManagerFactory: KeyManagerFactoryWrapper,
                              trustManagerFactory: TrustManagerFactoryWrapper) extends SSLContextBuilder {

  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def build(): SSLContext = {
    val protocol = info.protocol.getOrElse(Protocols.recommendedProtocol)

    val certificateValidator = buildCertificateValidator(info)

    val disabledAlgorithms = info.disabledAlgorithms.getOrElse(Algorithms.disabledAlgorithms)

    val keyManagers: Seq[KeyManager] = info.keyManagerConfig.map {
      val constraints = parseAll(line, disabledAlgorithms).get.toSet
      kmc => Seq(buildCompositeKeyManager(constraints, kmc))
    }.getOrElse(Nil)

    val trustManagers: Seq[TrustManager] = info.trustManagerConfig.map {
      val constraints = parseAll(line, disabledAlgorithms).get.toSet
      tmc => Seq(buildCompositeTrustManager(constraints, tmc, certificateValidator))
    }.getOrElse(Nil)

    buildSSLContext(protocol, keyManagers, trustManagers, info.secureRandom)
  }

  def buildSSLContext(protocol: String,
                      keyManagers: Seq[KeyManager],
                      trustManagers: Seq[TrustManager],
                      secureRandom: Option[SecureRandom]) = {
    val builder = new SimpleSSLContextBuilder(protocol, keyManagers, trustManagers, secureRandom)
    builder.build()
  }

  def buildCertificateValidator(info: SSLConfig): CertificateValidator = {

    val disabledAlgorithms = info.disabledAlgorithms.getOrElse(Algorithms.disabledAlgorithms)
    val disableCheckRevocation = info.loose.flatMap(_.disableCheckRevocation).getOrElse(false)

    val constraints = parseAll(line, disabledAlgorithms).get.toSet
    new CertificateValidator(constraints, revocationEnabled = !disableCheckRevocation)
  }

  def buildCompositeKeyManager(disabledAlgorithms: Set[AlgorithmConstraint], keyManagerConfig: KeyManagerConfig) = {
    val keyManagers = keyManagerConfig.keyStoreConfigs.map {
      ksc =>
        buildKeyManager(disabledAlgorithms, ksc)
    }
    new CompositeX509KeyManager(keyManagers)
  }

  def buildCompositeTrustManager(constraints: Set[AlgorithmConstraint], trustManagerInfo: TrustManagerConfig, certificateValidator: CertificateValidator) = {
    val trustManagers = trustManagerInfo.trustStoreConfigs.map {
      tsc =>
        buildTrustManager(constraints, tsc)
    }

    new CompositeX509TrustManager(trustManagers, certificateValidator)
  }

  // Get either a string or file based keystore builder from config.
  def keyStoreBuilder(ksc: KeyStoreConfig): KeyStoreBuilder = {
    val storeType = ksc.storeType.getOrElse(KeyStore.getDefaultType)
    val password = ksc.password.map(_.toCharArray)
    ksc.filePath.map {
      f =>
        fileBuilder(storeType, f, password)
    }.getOrElse {
      val data = ksc.data.getOrElse(throw new IllegalStateException("No keystore builder found!"))
      stringBuilder(storeType, data, password)
    }
  }

  def trustStoreBuilder(tsc: TrustStoreConfig): KeyStoreBuilder = {
    val storeType = tsc.storeType.getOrElse(KeyStore.getDefaultType)
    tsc.filePath.map {
      f =>
        fileBuilder(storeType, f, None)
    }.getOrElse {
      val data = tsc.data.getOrElse(throw new IllegalStateException("No truststore builder found!"))
      stringBuilder(storeType, data, None)
    }
  }

  def fileBuilder(storeType: String, filePath: String, password: Option[Array[Char]]): KeyStoreBuilder = {
    new FileBasedKeyStoreBuilder(storeType, filePath, password)
  }

  def stringBuilder(storeType: String, data: String, password: Option[Array[Char]]): KeyStoreBuilder = {
    new StringBasedKeyStoreBuilder(storeType, data, password)
  }

  /**
   * Builds a key manager from a keystore, using the KeyManagerFactory.
   */
  def buildKeyManager(disabledAlgorithms: Set[AlgorithmConstraint], ksc: KeyStoreConfig): X509KeyManager = {
    val keyStore = keyStoreBuilder(ksc).build()

    val password = ksc.password.map(_.toCharArray)

    val factory = keyManagerFactory
    try {
      validateKeyStore(disabledAlgorithms, ksc, keyStore)

      factory.init(keyStore, password.orNull)
    } catch {
      case e: UnrecoverableKeyException =>
        e.getCause
    }

    // The JSSE implementation only sends back ONE key manager, X509ExtendedKeyManager
    factory.getKeyManagers.head.asInstanceOf[X509KeyManager]
  }

  /**
   * Builds a trust manager.  A trust manager determines whether the remote auth credentials should be trusted.
   */

  // http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#Troubleshooting
  // XXX Attempt to store trusted certificates in PKCS12 keystore throws java.security.KeyStoreException:
  // TrustedCertEntry not supported.
  //  trusted certificates in pkcs12 keystore

  /**
   * Builds trust managers, using a TrustManagerFactory internally.
   */
  def buildTrustManager(constraints: Set[AlgorithmConstraint], tsc: TrustStoreConfig): X509TrustManager = {
    val factory = trustManagerFactory
    val certSelect: X509CertSelector = new X509CertSelector

    val trustStore = trustStoreBuilder(tsc).build()
    val pkixParameters = new PKIXBuilderParameters(trustStore, certSelect)

    // XXX Fix this so it is configurable
    val revocationEnabled = true
    pkixParameters.setRevocationEnabled(revocationEnabled)

    validateTrustStore(constraints, tsc, trustStore)

    factory.init(new CertPathTrustManagerParameters(pkixParameters))

    // The JSSE implementation only sends back ONE trust manager, X509TrustManager
    factory.getTrustManagers.head.asInstanceOf[X509TrustManager]
  }

  /**
   * Tests each trusted certificate in the store, and warns if the certificate is not valid.  Does not throw
   * exceptions.
   */
  def validateTrustStore(constraints: Set[AlgorithmConstraint], tsc: TrustStoreConfig, store: KeyStore) {
    import scala.collection.JavaConverters._
    logger.debug(s"validateTrustStore: store = $store, type = ${store.getType}, size = ${store.size}")
    store.aliases().asScala.foreach {
      alias =>
        Option(store.getCertificate(alias)).map {
          x509Cert =>
            try {
              validateCertificate(constraints, x509Cert)
            } catch {
              case e: CertificateException =>
                logger.warn(s"validateTrustStore: Skipping failed certificate with alias $alias from $tsc: " + e.getMessage)
                store.deleteEntry(alias)
            }
        }
    }
  }

  /**
   * Tests each trusted certificate in the store, and warns if the certificate is not valid.  Does not throw
   * exceptions.
   */
  def validateKeyStore(disabledAlgorithms: Set[AlgorithmConstraint], ksc: KeyStoreConfig, store: KeyStore) {
    import scala.collection.JavaConverters._
    logger.debug(s"validateKeyStore: store = $store, type = ${store.getType}, size = ${store.size}")
    store.aliases().asScala.foreach {
      alias =>
        Option(store.getCertificate(alias)).map {
          c =>
            try {
              validateCertificate(disabledAlgorithms, c)
            } catch {
              case e: CertificateException =>
                logger.warn(s"validateKeyStore: Skipping failed certificate with alias $alias from $ksc:" + e.getMessage)
                store.deleteEntry(alias)
            }
        }
    }
  }

  def validateCertificate(disabledAlgorithms: Set[AlgorithmConstraint], x509Cert: X509Certificate) {
    x509Cert.checkValidity()

    val key = x509Cert.getPublicKey
    val keySize = Algorithms.keySize(key)

    val lowerCaseAlgorithmName: String = x509Cert.getSigAlgName.toLowerCase(Locale.ENGLISH)
    logger.debug(s"validateTrustStore: lowerCaseAlgorithmName = $lowerCaseAlgorithmName, keySize = $keySize, disabledAlgorithms = $disabledAlgorithms")

    for (a <- Algorithms.decomposes(lowerCaseAlgorithmName)) {
      for (constraint <- disabledAlgorithms)
        if (constraint.matches(a, keySize)) {
          logger.debug(s"validateTrustStore: cert = $x509Cert failed on constraint $constraint")
          val msg = s"Certificate failed: algorithm $a with keySize $keySize matched disabledAlgorithm constraint $constraint"
          throw new CertificateException(msg)
        }
    }
  }

}
