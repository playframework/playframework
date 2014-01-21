/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import javax.net.ssl._
import java.security.{ UnrecoverableKeyException, KeyStore, SecureRandom }
import java.security.cert._
import java.util.Locale

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

  def getTrustManagers: Array[TrustManager] = instance.getTrustManagers
}

/**
 * Creates an SSL context builder from info objects.
 */
class ConfigSSLContextBuilder(info: SSLConfig,
    keyManagerFactory: KeyManagerFactoryWrapper,
    trustManagerFactory: TrustManagerFactoryWrapper) extends SSLContextBuilder {

  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def build: SSLContext = {
    val protocol = info.protocol.getOrElse(Protocols.recommendedProtocol)


    val disableCheckRevocation = info.loose.exists(_.disableCheckRevocation.getOrElse(false))
    val checkRevocation = !disableCheckRevocation

    val keyManagers: Seq[KeyManager] = info.keyManagerConfig.map {
      kmc => Seq(buildCompositeKeyManager(kmc))
    }.getOrElse(Nil)

    val certificateValidator = buildCertificateValidator(info)
    val trustManagers: Seq[TrustManager] = info.trustManagerConfig.map {
      tmc => Seq(buildCompositeTrustManager(tmc, certificateValidator, checkRevocation))
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

    val constraints = AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.line, disabledAlgorithms).get.toSet
    new CertificateValidator(constraints, revocationEnabled = !disableCheckRevocation)
  }

  def buildCompositeKeyManager(keyManagerConfig: KeyManagerConfig) = {
    val keyManagers = keyManagerConfig.keyStoreConfigs.map {
      ksc =>
        buildKeyManager(ksc)
    }
    new CompositeX509KeyManager(keyManagers)
  }

  def buildCompositeTrustManager(trustManagerInfo: TrustManagerConfig, certificateValidator: CertificateValidator, checkRevocation: Boolean) = {
    val trustManagers = trustManagerInfo.trustStoreConfigs.map {
      tsc =>
        buildTrustManager(tsc, checkRevocation)
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
  def buildKeyManager(ksc: KeyStoreConfig): X509KeyManager = {
    val keyStore = keyStoreBuilder(ksc).build()

    val password = ksc.password.map(_.toCharArray)

    val factory = keyManagerFactory
    try {
      validateKeyStore(ksc, keyStore)

      factory.init(keyStore, password.orNull)
    } catch {
      case e: UnrecoverableKeyException =>
        e.getCause
    }

    val keyManagers = factory.getKeyManagers
    if (keyManagers == null) {
      val msg = s"Cannot create key manager with configuration $ksc"
      throw new IllegalStateException(msg)
    }

    // The JSSE implementation only sends back ONE key manager, X509ExtendedKeyManager
    keyManagers.head.asInstanceOf[X509KeyManager]
  }

  /**
   * Builds trust managers, using a TrustManagerFactory internally.
   */
  def buildTrustManager(tsc: TrustStoreConfig, revocationEnabled: Boolean): X509TrustManager = {
    val factory = trustManagerFactory
    val certSelect: X509CertSelector = new X509CertSelector

    val trustStore = trustStoreBuilder(tsc).build()
    validateTrustStore(tsc, trustStore)

    val pkixParameters = new PKIXBuilderParameters(trustStore, certSelect)
    pkixParameters.setRevocationEnabled(revocationEnabled)

    factory.init(new CertPathTrustManagerParameters(pkixParameters))
    val trustManagers = factory.getTrustManagers
    if (trustManagers == null) {
      val msg = s"Cannot create trust manager with configuration $tsc"
      throw new IllegalStateException(msg)
    }

    // The JSSE implementation only sends back ONE trust manager, X509TrustManager
    trustManagers.head.asInstanceOf[X509TrustManager]
  }

  /**
   * Tests each trusted certificate in the store, and warns if the certificate is not valid.  Does not throw
   * exceptions.
   */
  def validateTrustStore(config: TrustStoreConfig, store: KeyStore) {
    import scala.collection.JavaConverters._
    logger.debug(s"validateTrustStore: store = $store, type = ${store.getType}, size = ${store.size}")

    //val checker = createAlgorithmChecker(constraints)
    store.aliases().asScala.foreach {
      alias =>
        Option(store.getCertificate(alias)).map {
          x509Cert =>
            try {
              validateCertificate(x509Cert)
            } catch {
              case e: CertificateNotYetValidException =>
                logger.warn(s"validateTrustStore: Skipping not yet valid certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: CertificateExpiredException =>
                logger.warn(s"validateTrustStore: Skipping expired certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: CertificateException =>
                logger.warn(s"validateTrustStore: Skipping bad certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: Exception =>
                logger.warn(s"validateTrustStore: Skipping unknown exception alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
            }
        }
    }

    // We completely emptied out the keystore.  That's going to fail when we pass it in to the manager.
    if (store.size() == 0) {
      throw new IllegalStateException(s"There are no valid certificates in key store: $config")
    }
  }

  /**
   * Tests each trusted certificate in the store, and warns if the certificate is not valid.  Does not throw
   * exceptions.
   */
  def validateKeyStore(config: KeyStoreConfig, store: KeyStore) {
    import scala.collection.JavaConverters._
    logger.debug(s"validateKeyStore: store = $store, type = ${store.getType}, size = ${store.size}")

    //val checker = createAlgorithmChecker(constraints)
    store.aliases().asScala.foreach {
      alias =>
        Option(store.getCertificate(alias)).map {
          c =>
            try {
              validateCertificate(c)
            } catch {
              case e: CertificateNotYetValidException =>
                logger.warn(s"validateKeyStore: Skipping not yet valid certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: CertificateExpiredException =>
                logger.warn(s"validateKeyStore: Skipping expired certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: CertificateException =>
                logger.warn(s"validateKeyStore: Skipping bad certificate with alias $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
              case e: Exception =>
                logger.warn(s"validateKeyStore: Skipping unknown exception $alias from $config: " + e.getMessage)
                store.deleteEntry(alias)
            }
        }
    }

    // We completely emptied out the keystore.  That's going to fail when we pass it in to the manager.
    if (store.size() == 0) {
      throw new IllegalStateException(s"There are no valid certificates in key store: $config")
    }
  }

  //def createAlgorithmChecker(constraints:Set[AlgorithmConstraint]) : AlgorithmChecker = new AlgorithmChecker(constraints)

  def validateCertificate(x509Cert: X509Certificate) {
    x509Cert.checkValidity()
    //algorithmChecker.check(x509Cert, unresolvedCritExts = java.util.Collections.emptySet())
  }
}
