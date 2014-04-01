/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import javax.net.ssl._
import java.security.{ UnrecoverableKeyException, KeyStore, SecureRandom }
import java.security.cert._
import java.io._
import java.net.URL

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

    val keyManagers: Seq[KeyManager] = info.keyManagerConfig.map {
      kmc => Seq(buildCompositeKeyManager(kmc))
    }.getOrElse(Nil)

    val checkRevocation = info.checkRevocation.getOrElse(false)

    val revocationLists = certificateRevocationList(info)

    val disabledSignatureAlgorithms = info.disabledSignatureAlgorithms.getOrElse(Algorithms.disabledSignatureAlgorithms)
    val signatureConstraints = AlgorithmConstraintsParser(disabledSignatureAlgorithms).toSet

    val disabledKeyAlgorithms = info.disabledKeyAlgorithms.getOrElse(Algorithms.disabledKeyAlgorithms)
    val keyConstraints = AlgorithmConstraintsParser(disabledKeyAlgorithms).toSet

    val trustManagers: Seq[TrustManager] = info.trustManagerConfig.map {
      tmc => Seq(buildCompositeTrustManager(tmc, checkRevocation, revocationLists, signatureConstraints, keyConstraints))
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

  def buildCompositeKeyManager(keyManagerConfig: KeyManagerConfig) = {
    val keyManagers = keyManagerConfig.keyStoreConfigs.map {
      ksc =>
        buildKeyManager(ksc)
    }
    new CompositeX509KeyManager(keyManagers)
  }

  def buildCompositeTrustManager(trustManagerInfo: TrustManagerConfig,
    revocationEnabled: Boolean,
    revocationLists: Option[Seq[CRL]],
    signatureConstraints: Set[AlgorithmConstraint],
    keyConstraints: Set[AlgorithmConstraint]) = {

    val trustManagers = trustManagerInfo.trustStoreConfigs.map {
      tsc =>
        buildTrustManager(tsc, revocationEnabled, revocationLists, signatureConstraints, keyConstraints)
    }
    new CompositeX509TrustManager(trustManagers)
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

  // Should anyone have any interest in implementing this feature at all, they can implement this method and
  // submit a patch.
  def certificateRevocationList(sslConfig: SSLConfig): Option[Seq[CRL]] = {
    sslConfig.revocationLists.map {
      urls =>
        urls.map(generateCRLFromURL)
    }
  }

  def generateCRL(inputStream: InputStream): CRL = {
    val cf = CertificateFactory.getInstance("X509")
    val crl = cf.generateCRL(inputStream).asInstanceOf[X509CRL]
    crl
  }

  def generateCRLFromURL(url: URL): CRL = {
    val connection = url.openConnection()
    connection.setDoInput(true)
    connection.setUseCaches(false)
    val inStream = new DataInputStream(connection.getInputStream)
    try {
      generateCRL(inStream)
    } finally {
      inStream.close()
    }
  }

  def generateCRLFromFile(file: File): CRL = {
    val fileStream = new BufferedInputStream(new FileInputStream(file))
    val inStream = new DataInputStream(fileStream)
    try {
      generateCRL(inStream)
    } finally {
      inStream.close()
    }
  }

  def buildTrustManagerParameters(trustStore: KeyStore,
    revocationEnabled: Boolean,
    revocationLists: Option[Seq[CRL]],
    signatureConstraints: Set[AlgorithmConstraint],
    keyConstraints: Set[AlgorithmConstraint]): CertPathTrustManagerParameters = {
    import scala.collection.JavaConverters._

    val certSelect: X509CertSelector = new X509CertSelector
    val pkixParameters = new PKIXBuilderParameters(trustStore, certSelect)
    pkixParameters.setRevocationEnabled(revocationEnabled)

    // For the sake of completeness, set the static revocation list if it exists...
    revocationLists.map {
      crlList =>
        import scala.collection.JavaConverters._
        pkixParameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crlList.asJavaCollection)))
    }

    // Add the algorithm checker in here...
    val checkers: Seq[PKIXCertPathChecker] = Seq(
      new AlgorithmChecker(signatureConstraints, keyConstraints)
    )

    // Use the custom cert path checkers we defined...
    pkixParameters.setCertPathCheckers(checkers.asJava)
    new CertPathTrustManagerParameters(pkixParameters)
  }

  /**
   * Builds trust managers, using a TrustManagerFactory internally.
   */
  def buildTrustManager(tsc: TrustStoreConfig,
    revocationEnabled: Boolean,
    revocationLists: Option[Seq[CRL]],
    signatureConstraints: Set[AlgorithmConstraint],
    keyConstraints: Set[AlgorithmConstraint]): X509TrustManager = {

    val factory = trustManagerFactory
    val trustStore = trustStoreBuilder(tsc).build()
    val trustManagerParameters = buildTrustManagerParameters(
      trustStore,
      revocationEnabled,
      revocationLists,
      signatureConstraints,
      keyConstraints
    )

    factory.init(trustManagerParameters)
    val trustManagers = factory.getTrustManagers
    if (trustManagers == null) {
      val msg = s"Cannot create trust manager with configuration $tsc"
      throw new IllegalStateException(msg)
    }

    // The JSSE implementation only sends back ONE trust manager, X509TrustManager
    trustManagers.head.asInstanceOf[X509TrustManager]
  }

}
