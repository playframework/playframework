/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ning

import play.api.libs.ws.WSClientConfig
import com.ning.http.client.{ SSLEngineFactory, AsyncHttpClientConfig }
import javax.net.ssl._
import play.api.libs.ws.ssl._
import org.slf4j.LoggerFactory
import java.security.KeyStore
import java.security.cert.CertPathValidatorException

/**
 * Builds a valid AsyncHttpClientConfig object from config.
 *
 * @param config the client configuration.
 * @param builder a builder, defaults to a new instance.  You can pass in a preconfigured builder here.
 */
class NingAsyncHttpClientConfigBuilder(config: WSClientConfig,
    builder: AsyncHttpClientConfig.Builder = new AsyncHttpClientConfig.Builder()) {

  private[ning] val logger = LoggerFactory.getLogger(this.getClass)

  def build(): AsyncHttpClientConfig = {
    configureWS(config)

    val acceptAnyCertificate = config.acceptAnyCertificate.getOrElse(false)
    configureSSL(acceptAnyCertificate, config.ssl.getOrElse(DefaultSSLConfig()))

    builder.build()
  }

  /**
   * Configures the global settings.
   */
  def configureWS(config: WSClientConfig) {
    import play.api.libs.ws.Defaults._
    builder.setConnectionTimeoutInMs(config.connectionTimeout.getOrElse(connectionTimeout).toInt)
      .setIdleConnectionTimeoutInMs(config.idleTimeout.getOrElse(idleTimeout).toInt)
      .setRequestTimeoutInMs(config.requestTimeout.getOrElse(requestTimeout).toInt)
      .setFollowRedirects(config.followRedirects.getOrElse(followRedirects))
      .setUseProxyProperties(config.useProxyProperties.getOrElse(useProxyProperties))
      .setCompressionEnabled(config.compressionEnabled.getOrElse(compressionEnabled))

    config.userAgent.map {
      useragent =>
        builder.setUserAgent(useragent)
    }
  }

  def configureProtocols(existingProtocols: Array[String], sslConfig: SSLConfig): Array[String] = {
    val definedProtocols = sslConfig.enabledProtocols match {
      case Some(configuredProtocols) =>
        // If we are given a specific list of protocols, then return it in exactly that order,
        // assuming that it's actually possible in the SSL context.
        configuredProtocols.filter(existingProtocols.contains).toArray

      case None =>
        // Otherwise, we return the default protocols in the given list.
        Protocols.recommendedProtocols.filter(existingProtocols.contains).toArray
    }

    val allowWeakProtocols = sslConfig.loose.exists(loose => loose.allowWeakProtocols.getOrElse(false))
    if (!allowWeakProtocols) {
      val deprecatedProtocols = Protocols.deprecatedProtocols
      for (deprecatedProtocol <- deprecatedProtocols) {
        if (definedProtocols.contains(deprecatedProtocol)) {
          throw new IllegalStateException(s"Weak protocol $deprecatedProtocol found in ws.ssl.protocols!")
        }
      }
    }
    definedProtocols
  }

  def configureCipherSuites(existingCiphers: Array[String], sslConfig: SSLConfig): Array[String] = {
    val definedCiphers = sslConfig.enabledCipherSuites match {
      case Some(configuredCiphers) =>
        // If we are given a specific list of ciphers, return it in that order.
        configuredCiphers.filter(existingCiphers.contains(_)).toArray

      case None =>
        Ciphers.recommendedCiphers.filter(existingCiphers.contains(_)).toArray
    }

    val allowWeakCiphers = sslConfig.loose.exists(loose => loose.allowWeakCiphers.getOrElse(false))
    if (!allowWeakCiphers) {
      val deprecatedCiphers = Ciphers.deprecatedCiphers
      for (deprecatedCipher <- deprecatedCiphers) {
        if (definedCiphers.contains(deprecatedCipher)) {
          throw new IllegalStateException(s"Weak cipher $deprecatedCipher found in ws.ssl.ciphers!")
        }
      }
    }
    definedCiphers
  }

  /**
   * Configures the SSL.  Can use the system SSLContext.getDefault() if "ws.ssl.default" is set.
   */
  def configureSSL(acceptAnyCertificate: Boolean, sslConfig: SSLConfig) {

    // context!
    val sslContext = configureContext(acceptAnyCertificate, sslConfig)

    // Set up the engine parameters -- this is cloned, and changes are applied to the SSLEngine.
    val engineParams = sslContext.getDefaultSSLParameters

    // protocols!
    val defaultProtocols = engineParams.getProtocols
    val protocols = configureProtocols(defaultProtocols, sslConfig)
    engineParams.setProtocols(protocols)

    // ciphers!
    val defaultCiphers = engineParams.getCipherSuites
    val cipherSuites = configureCipherSuites(defaultCiphers, sslConfig)
    engineParams.setCipherSuites(cipherSuites)

    // Hostname Processing
    val disableHostnameVerification = sslConfig.loose.flatMap(_.disableHostnameVerification).getOrElse(false)
    if (disableHostnameVerification) {
      logger.warn("buildHostnameVerifier: disabling hostname verification because disableHostnameVerification is set")
    } else if (acceptAnyCertificate) {
      logger.warn("buildHostnameVerifier: disabling hostname verification because acceptAnyCertificate is set")
    } else {
      val hostnameVerifier = buildHostnameVerifier(sslConfig)
      builder.setHostnameVerifier(hostnameVerifier)
    }

    builder.setSSLContext(sslContext)

    // Must set SSL engine factory AFTER the ssl context...
    val sslEngineFactory = new DefaultSSLEngineFactory(sslContext, engineParams)
    builder.setSSLEngineFactory(sslEngineFactory)
  }

  def configureContext(acceptAnyCertificate: Boolean, sslConfig: SSLConfig): SSLContext = {
    val useDefault = sslConfig.default.getOrElse(false)

    if (useDefault) {
      logger.info("configureContext: ws.ssl.default is true, using default SSLContext")
      validateDefaultTrustManager(sslConfig)
      SSLContext.getDefault
    } else {
      // break out the static methods as much as we can...
      val keyManagerFactory = buildKeyManagerFactory(sslConfig)
      val trustManagerFactory = buildTrustManagerFactory(acceptAnyCertificate, sslConfig)
      new ConfigSSLContextBuilder(sslConfig, keyManagerFactory, trustManagerFactory, acceptAnyCertificate).build()
    }
  }

  def buildKeyManagerFactory(ssl: SSLConfig): KeyManagerFactoryWrapper = {
    val keyManagerAlgorithm = ssl.keyManagerConfig.flatMap(_.algorithm).getOrElse(KeyManagerFactory.getDefaultAlgorithm)
    new DefaultKeyManagerFactoryWrapper(keyManagerAlgorithm)
  }

  def buildTrustManagerFactory(acceptAnyCertificate: Boolean, ssl: SSLConfig): TrustManagerFactoryWrapper = {
    if (acceptAnyCertificate) {
      logger.warn("buildTrustManagerFactory: ws.acceptAnyCertificate is true, using AcceptAnyCertificateTrustManagerFactoryWrapper")

      new AcceptAnyCertificateTrustManagerFactoryWrapper()
    } else {
      val trustManagerAlgorithm = ssl.trustManagerConfig.flatMap(_.algorithm).getOrElse(TrustManagerFactory.getDefaultAlgorithm)
      new DefaultTrustManagerFactoryWrapper(trustManagerAlgorithm)
    }
  }

  def buildHostnameVerifier(sslConfig: SSLConfig): HostnameVerifier = {
    val hostnameVerifierClass = sslConfig.hostnameVerifierClass.getOrElse(classOf[DefaultHostnameVerifier])
    logger.debug("buildHostnameVerifier: enabling hostname verification using {}", hostnameVerifierClass)

    try {
      hostnameVerifierClass.newInstance()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Cannot configure hostname verifier", e)
    }
  }

  def validateDefaultTrustManager(sslConfig: SSLConfig) {
    // If we are using a default SSL context, we can't filter out certificates with weak algorithms
    // We ALSO don't have access to the trust manager from the SSLContext without doing horrible things
    // with reflection.
    //
    // However, given that the default SSLContextImpl will call out to the TrustManagerFactory and any
    // configuration with system properties will also apply with the factory, we can use the factory
    // method to recreate the trust manager and validate the trust certificates that way.
    //
    // This is really a last ditch attempt to satisfy https://wiki.mozilla.org/CA:MD5and1024 on root certificates.
    //
    // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/sun/security/ssl/SSLContextImpl.java#79

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(null.asInstanceOf[KeyStore])
    val trustManager: X509TrustManager = tmf.getTrustManagers()(0).asInstanceOf[X509TrustManager]

    val disabledKeyAlgorithms = sslConfig.disabledKeyAlgorithms.getOrElse(Algorithms.disabledKeyAlgorithms)
    val constraints = AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.line, disabledKeyAlgorithms).get.toSet
    val algorithmChecker = new AlgorithmChecker(keyConstraints = constraints, signatureConstraints = Set())
    for (cert <- trustManager.getAcceptedIssuers) {
      try {
        algorithmChecker.checkKeyAlgorithms(cert)
      } catch {
        case e: CertPathValidatorException =>
          logger.warn("You are using ws.ssl.default=true and have a weak certificate in your default trust store: " + e.getMessage)
      }
    }
  }

  /**
   * Factory that creates an SSLEngine.
   */
  class DefaultSSLEngineFactory(sslContext: SSLContext, sslParameters: SSLParameters) extends SSLEngineFactory {
    def newSSLEngine(): SSLEngine = {
      val sslEngine = sslContext.createSSLEngine()
      sslEngine.setSSLParameters(sslParameters)
      sslEngine.setUseClientMode(true)
      sslEngine
    }
  }
}

