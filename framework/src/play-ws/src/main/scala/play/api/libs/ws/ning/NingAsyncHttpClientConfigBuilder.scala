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
import scala.util.control.NonFatal

/**
 * Builds a valid AsyncHttpClientConfig object from config.
 *
 * @param config the client configuration.
 * @param builder a builder, defaults to a new instance.  You can pass in a preconfigured builder here.
 */
class NingAsyncHttpClientConfigBuilder(config: WSClientConfig,
    builder: AsyncHttpClientConfig.Builder = new AsyncHttpClientConfig.Builder()) {

  val defaultHostnameVerifierClassName = "play.api.libs.ws.ssl.DefaultHostnameVerifier"

  private val logger = LoggerFactory.getLogger(this.getClass)

  def build(): AsyncHttpClientConfig = {
    configureWS(config)

    config.acceptAnyCertificate match {
      case Some(true) =>
      // lean on the AsyncHttpClient bug

      case _ =>
        configureSSL(config.ssl.getOrElse(DefaultSSLConfig()))
    }
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
  def configureSSL(sslConfig: SSLConfig) {

    // context!
    val useDefault = sslConfig.default.getOrElse(false)
    val sslContext = if (useDefault) {
      logger.info("buildSSLContext: ws.ssl.default is true, using default SSLContext")
      validateDefaultTrustManager(sslConfig)
      SSLContext.getDefault
    } else {
      // break out the static methods as much as we can...
      val keyManagerFactory = buildKeyManagerFactory(sslConfig)
      val trustManagerFactory = buildTrustManagerFactory(sslConfig)
      new ConfigSSLContextBuilder(sslConfig, keyManagerFactory, trustManagerFactory).build()
    }

    // protocols!
    val defaultParams = sslContext.getDefaultSSLParameters
    val defaultProtocols = defaultParams.getProtocols
    val protocols = configureProtocols(defaultProtocols, sslConfig)
    defaultParams.setProtocols(protocols)

    // ciphers!
    val defaultCiphers = defaultParams.getCipherSuites
    val cipherSuites = configureCipherSuites(defaultCiphers, sslConfig)
    defaultParams.setCipherSuites(cipherSuites)

    val sslEngineFactory = new DefaultSSLEngineFactory(sslConfig, sslContext, enabledProtocols = protocols, enabledCipherSuites = cipherSuites)

    // Hostname Processing
    val disableHostnameVerification = sslConfig.loose.flatMap(_.disableHostnameVerification).getOrElse(false)
    if (!disableHostnameVerification) {
      val hostnameVerifier = buildHostnameVerifier(sslConfig)
      builder.setHostnameVerifier(hostnameVerifier)
    } else {
      logger.warn("buildHostnameVerifier: disabling hostname verification")
    }

    builder.setSSLContext(sslContext)

    // Must set SSL engine factory AFTER the ssl context...
    builder.setSSLEngineFactory(sslEngineFactory)
  }

  def buildKeyManagerFactory(ssl: SSLConfig): KeyManagerFactoryWrapper = {
    val keyManagerAlgorithm = ssl.keyManagerConfig.flatMap(_.algorithm).getOrElse(KeyManagerFactory.getDefaultAlgorithm)
    new DefaultKeyManagerFactoryWrapper(keyManagerAlgorithm)
  }

  def buildTrustManagerFactory(ssl: SSLConfig): TrustManagerFactoryWrapper = {
    val trustManagerAlgorithm = ssl.trustManagerConfig.flatMap(_.algorithm).getOrElse(TrustManagerFactory.getDefaultAlgorithm)
    new DefaultTrustManagerFactoryWrapper(trustManagerAlgorithm)
  }

  def buildHostnameVerifier(sslConfig: SSLConfig): HostnameVerifier = {
    val hostnameVerifierClassName = sslConfig.hostnameVerifierClassName.getOrElse(defaultHostnameVerifierClassName)
    logger.debug("buildHostnameVerifier: enabling hostname verification using {}", hostnameVerifierClassName)
    // Provide people with the option of using their own hostname verifier, and set the apache internal
    // hostname verifier as the default.

    try {
      val verifierClass = Class.forName(hostnameVerifierClassName)
      verifierClass.newInstance().asInstanceOf[HostnameVerifier]
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
    // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/sun/security/ssl/SSLContextImpl.java#79

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(null.asInstanceOf[KeyStore])
    val trustManager: X509TrustManager = tmf.getTrustManagers()(0).asInstanceOf[X509TrustManager]

    //val disabledAlgorithms = sslConfig.disabledAlgorithms.getOrElse(Algorithms.disabledAlgorithms)
    //val constraints = AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.line, disabledAlgorithms).get.toSet
    //val algorithmChecker = new AlgorithmChecker(constraints)
    try {
      for (cert <- trustManager.getAcceptedIssuers) {
        cert.checkValidity()
        //algorithmChecker.check(cert, unresolvedCritExts = java.util.Collections.emptySet())
      }
    } catch {
      case NonFatal(e) =>
        val msg = "You have ws.ssl.default = true in your configuration, and you have an invalid certificate in your trust store."
        throw new IllegalStateException(msg, e)
    }
  }

  /**
   * Factory that creates an SSLEngine.
   */
  class DefaultSSLEngineFactory(config: SSLConfig,
      sslContext: SSLContext,
      enabledProtocols: Array[String],
      enabledCipherSuites: Array[String]) extends SSLEngineFactory {
    def newSSLEngine(): SSLEngine = {
      val sslEngine = sslContext.createSSLEngine()
      sslEngine.setSSLParameters(sslContext.getDefaultSSLParameters)
      sslEngine.setEnabledProtocols(enabledProtocols)
      sslEngine.setEnabledCipherSuites(enabledCipherSuites)
      sslEngine.setUseClientMode(true)
      sslEngine
    }
  }
}

