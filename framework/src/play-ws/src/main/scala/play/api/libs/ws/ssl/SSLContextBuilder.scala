/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import javax.net.ssl._
import java.security._
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

  @throws[KeyStoreException]
  @throws[NoSuchAlgorithmException]
  @throws[UnrecoverableKeyException]
  def init(keystore: KeyStore, password: Array[Char]): Unit

  def getKeyManagers: Array[KeyManager]
}

trait TrustManagerFactoryWrapper {

  @throws[InvalidAlgorithmParameterException]
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

    val revocationLists = certificateRevocationList(info)
    val signatureConstraints = info.disabledSignatureAlgorithms.map(AlgorithmConstraintsParser.apply).toSet

    val keySizeConstraints = info.disabledKeyAlgorithms.map(AlgorithmConstraintsParser.apply).toSet

    val algorithmChecker = new AlgorithmChecker(signatureConstraints, keySizeConstraints)

    val keyManagers: Seq[KeyManager] = if (info.keyManagerConfig.keyStoreConfigs.nonEmpty) {
      Seq(buildCompositeKeyManager(info.keyManagerConfig, algorithmChecker))
    } else Nil

    val trustManagers: Seq[TrustManager] = if (info.trustManagerConfig.trustStoreConfigs.nonEmpty) {
      Seq(buildCompositeTrustManager(info.trustManagerConfig, info.checkRevocation.getOrElse(false), revocationLists, algorithmChecker))
    } else Nil

    buildSSLContext(info.protocol, keyManagers, trustManagers, info.secureRandom)
  }

  def buildSSLContext(protocol: String,
    keyManagers: Seq[KeyManager],
    trustManagers: Seq[TrustManager],
    secureRandom: Option[SecureRandom]) = {
    val builder = new SimpleSSLContextBuilder(protocol, keyManagers, trustManagers, secureRandom)
    builder.build()
  }

  def buildCompositeKeyManager(keyManagerConfig: KeyManagerConfig, algorithmChecker: AlgorithmChecker) = {
    val keyManagers = keyManagerConfig.keyStoreConfigs.map {
      ksc =>
        buildKeyManager(ksc, algorithmChecker)
    }
    new CompositeX509KeyManager(keyManagers)
  }

  def buildCompositeTrustManager(trustManagerInfo: TrustManagerConfig,
    revocationEnabled: Boolean,
    revocationLists: Option[Seq[CRL]], algorithmChecker: AlgorithmChecker) = {

    val trustManagers = trustManagerInfo.trustStoreConfigs.map {
      tsc =>
        buildTrustManager(tsc, revocationEnabled, revocationLists, algorithmChecker)
    }
    new CompositeX509TrustManager(trustManagers, algorithmChecker)
  }

  // Get either a string or file based keystore builder from config.
  def keyStoreBuilder(ksc: KeyStoreConfig): KeyStoreBuilder = {
    val password = ksc.password.map(_.toCharArray)
    ksc.filePath.map { f =>
      fileBuilder(ksc.storeType, f, password)
    }.getOrElse {
      val data = ksc.data.getOrElse(throw new IllegalStateException("No keystore builder found!"))
      stringBuilder(data)
    }
  }

  def trustStoreBuilder(tsc: TrustStoreConfig): KeyStoreBuilder = {
    tsc.filePath.map { f =>
      fileBuilder(tsc.storeType, f, None)
    }.getOrElse {
      val data = tsc.data.getOrElse(throw new IllegalStateException("No truststore builder found!"))
      stringBuilder(data)
    }
  }

  def fileBuilder(storeType: String, filePath: String, password: Option[Array[Char]]): KeyStoreBuilder = {
    new FileBasedKeyStoreBuilder(storeType, filePath, password)
  }

  def stringBuilder(data: String): KeyStoreBuilder = {
    new StringBasedKeyStoreBuilder(data)
  }

  /**
   * Returns true if the keystore should throw an exception as a result of the JSSE bug 6879539, false otherwise.
   */
  def warnOnPKCS12EmptyPasswordBug(ksc: KeyStoreConfig): Boolean = {
    ksc.storeType.equalsIgnoreCase("pkcs12") && !ksc.password.exists(!_.isEmpty)
  }

  /**
   * Builds a key manager from a keystore, using the KeyManagerFactory.
   */
  def buildKeyManager(ksc: KeyStoreConfig, algorithmChecker: AlgorithmChecker): X509KeyManager = {
    val keyStore = try {
      keyStoreBuilder(ksc).build()
    } catch {
      case e: java.lang.ArithmeticException =>
        // This bug only exists in 1.6: we'll only check on 1.6 and explain after the exception.
        val willExplodeOnEmptyPassword = foldVersion(run16 = warnOnPKCS12EmptyPasswordBug(ksc), runHigher = false)
        if (willExplodeOnEmptyPassword) {
          val msg =
            """You are running JDK 1.6, have a PKCS12 keystore with a null or empty password, and have run into a JSSE bug.
              |The bug is closed in JDK 1.8, and backported to 1.7u4 / b13, so upgrading will fix this.
              |Please see: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6879539
            """.stripMargin
          throw new IllegalStateException(msg, e)
        } else {
          throw e
        }
      case bpe: javax.crypto.BadPaddingException =>
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6415637
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6974037
        // If you run into "Given final block not properly padded", then it's because you entered in the
        // wrong password for the keystore, and JSSE tries to decrypt and only then verify the MAC.
        throw new SecurityException("Mac verify error: invalid password?", bpe)
    }

    if (!validateStoreContainsPrivateKeys(ksc, keyStore)) {
      logger.warn(s"Client authentication is not possible as there are no private keys found in ${ksc.filePath}")
    }

    validateStore(keyStore, algorithmChecker)

    val password = ksc.password.map(_.toCharArray)

    val factory = keyManagerFactory
    try {
      factory.init(keyStore, password.orNull)
    } catch {
      case e: UnrecoverableKeyException =>
        logger.error(s"Unrecoverable key in keystore $ksc")
        throw new IllegalStateException(e)
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
    algorithmChecker: AlgorithmChecker): CertPathTrustManagerParameters = {
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

    // Add the algorithm checker in here to check the certification path sequence (not including trust anchor)...
    val checkers: Seq[PKIXCertPathChecker] = Seq(algorithmChecker)

    // Use the custom cert path checkers we defined...
    pkixParameters.setCertPathCheckers(checkers.asJava)
    new CertPathTrustManagerParameters(pkixParameters)
  }

  /**
   * Builds trust managers, using a TrustManagerFactory internally.
   */
  def buildTrustManager(tsc: TrustStoreConfig,
    revocationEnabled: Boolean,
    revocationLists: Option[Seq[CRL]], algorithmChecker: AlgorithmChecker): X509TrustManager = {

    val factory = trustManagerFactory
    val trustStore = trustStoreBuilder(tsc).build()
    validateStore(trustStore, algorithmChecker)

    val trustManagerParameters = buildTrustManagerParameters(
      trustStore,
      revocationEnabled,
      revocationLists,
      algorithmChecker)

    factory.init(trustManagerParameters)
    val trustManagers = factory.getTrustManagers
    if (trustManagers == null) {
      val msg = s"Cannot create trust manager with configuration $tsc"
      throw new IllegalStateException(msg)
    }

    // The JSSE implementation only sends back ONE trust manager, X509TrustManager
    trustManagers.head.asInstanceOf[X509TrustManager]
  }

  /**
   * Validates that a key store (as opposed to a trust store) contains private keys for client authentication.
   */
  def validateStoreContainsPrivateKeys(ksc: KeyStoreConfig, keyStore: KeyStore): Boolean = {
    import scala.collection.JavaConverters._

    // Is there actually a private key being stored in this key store?
    val password = ksc.password.map(_.toCharArray).orNull
    var containsPrivateKeys = false
    for (keyAlias <- keyStore.aliases().asScala) {
      val key = keyStore.getKey(keyAlias, password)
      key match {
        case privateKey: PrivateKey =>
          logger.debug(s"validateStoreContainsPrivateKeys: private key found for alias $keyAlias")
          containsPrivateKeys = true

        case otherKey =>
          // We want to warn on every failure, as this is not the correct setup for a key store.
          val msg = s"validateStoreContainsPrivateKeys: No private key found for alias $keyAlias, it cannot be used for client authentication"
          logger.warn(msg)
      }
    }
    containsPrivateKeys
  }

  /**
   * Tests each trusted certificate in the store, and warns if the certificate is not valid.  Does not throw
   * exceptions.
   */
  def validateStore(store: KeyStore, algorithmChecker: AlgorithmChecker) {
    import scala.collection.JavaConverters._
    logger.debug(s"validateStore: type = ${store.getType}, size = ${store.size}")

    store.aliases().asScala.foreach {
      alias =>
        Option(store.getCertificate(alias)).map {
          c =>
            try {
              algorithmChecker.checkKeyAlgorithms(c)
            } catch {
              case e: CertPathValidatorException =>
                logger.warn(s"validateStore: Skipping certificate with weak key size in $alias: " + e.getMessage)
                store.deleteEntry(alias)
              case e: Exception =>
                logger.warn(s"validateStore: Skipping unknown exception $alias: " + e.getMessage)
                store.deleteEntry(alias)
            }
        }
    }
  }

}
