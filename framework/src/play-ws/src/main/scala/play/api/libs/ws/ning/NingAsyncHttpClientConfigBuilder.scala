/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ning

import play.api.libs.ws.WSClientConfig
import com.ning.http.client.{SSLEngineFactory, AsyncHttpClientConfig}
import javax.net.ssl._
import play.api.libs.ws.ssl._
import org.slf4j.LoggerFactory

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
    configureSSL(config.ssl.getOrElse(DefaultSSLConfig()))
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

    // Filter out weak protocols from the resulting list.
    val allowWeakProtocols = sslConfig.loose.exists(loose => loose.allowWeakProtocols.getOrElse(false))
    if (allowWeakProtocols) {
      definedProtocols
    } else {
      val deprecatedProtocols = Protocols.deprecatedProtocols
      definedProtocols.filterNot(deprecatedProtocols.contains)
    }
  }

  def configureCipherSuites(existingCiphers: Array[String], sslConfig: SSLConfig): Array[String] = {
    val definedCiphers = sslConfig.enabledCipherSuites match {
      case Some(configuredCiphers) =>
        // If we are given a specific list of ciphers, return it in that order.
        configuredCiphers.filter(existingCiphers.contains(_)).toArray

      case None =>
        // Otherwise, we return the recommended ciphers
        Ciphers.recommendedCiphers.filter(existingCiphers.contains(_)).toArray
    }

    // Filter out weak ciphers from the resulting list.
    val allowWeakCiphers = sslConfig.loose.exists(loose => loose.allowWeakCiphers.getOrElse(false))
    if (allowWeakCiphers) {
      definedCiphers
    } else {
      val deprecatedCiphers = Ciphers.deprecatedCiphers
      definedCiphers.filterNot(deprecatedCiphers.contains(_))
    }
  }

  /**
   * Configures the SSL.  Can be disabled entirely if "ws.ssl.off" is set, or to use the system
   * SSLContext.getDefault() if "ws.ssl.default" is set.
   */
  def configureSSL(sslConfig: SSLConfig) {

    // By DEFAULT, AsyncHttpClient will accept ("trust") absolutely ANY certificate.
    //
    // AsyncHttpClient issue #352: "SSL/TLS certificate verification disabled"
    // https://github.com/AsyncHttpClient/async-http-client/issues/352
    //
    // From http://kevinlocke.name/bits/2012/10/03/ssl-certificate-verification-in-dispatch-and-asynchttpclient/
    //
    // "The SSLContext class is central to the SSL implementation in Java in general and in AsyncHttpClient in particular.
    // The default SSLContext for AsyncHttpClient is dependent on whether the javax.net.ssl.keyStore system property is set.
    // If this property is set, AsyncHttpClient will create a TLS SSLContext with a KeyManager based on the specified key
    // store (and configured based on the values of many other javax.net.ssl properties as described in the
    // JSEE Reference Guide linked above). Otherwise, it will create a TLS SSLContext with no KeyManager and a
    // TrustManager which accepts everything. In effect, if javax.net.ssl.keyStore is unspecified, any
    // ol’ SSL certificate will do."

    val isOff = sslConfig.off.getOrElse(false)
    if (isOff) {
      logger.warn("configureAsyncHttpConfigBuilder: ws.ssl.off is true, AsyncHttpClient will accept ANY SSL certificate!")
      return
    }

    // context!
    val useDefault = sslConfig.default.getOrElse(false)
    val sslContext = if (useDefault) {
      logger.info("buildSSLContext: ws.ssl.default is true, using default SSLContext")
      SSLContext.getDefault
    } else {
      // break out the static methods as much as we can...
      val keyManagerFactory = buildKeyManagerFactory(sslConfig)
      val trustManagerFactory = buildTrustManagerFactory(sslConfig)
      new ConfigSSLContextBuilder(sslConfig, keyManagerFactory, trustManagerFactory).build()
    }

    // protocols!
    //val allProtocols = sslContext.getSupportedSSLParameters.getProtocols
    //logger.info(s"allProtocols = ${allProtocols.toSeq}")
    val defaultParams = sslContext.getDefaultSSLParameters
    val defaultProtocols = defaultParams.getProtocols
    //logger.info(s"defaultProtocols = ${defaultProtocols.toSeq}")
    val protocols = configureProtocols(defaultProtocols, sslConfig)
    defaultParams.setProtocols(protocols)

    // ciphers!
    //val allCipherSuites = sslContext.getSupportedSSLParameters.getCipherSuites
    //logger.info(s"allCipherSuites = ${allCipherSuites.toSeq.mkString("\n")}")
    val defaultCiphers = defaultParams.getCipherSuites
    //logger.info(s"defaultCiphers = ${defaultCiphers.toSeq.mkString("\n")}")
    val cipherSuites = configureCipherSuites(defaultCiphers, sslConfig)
    defaultParams.setCipherSuites(cipherSuites)

    // XXX There are reports that the Ning SSL connection pool does not work as expected.
    // https://github.com/AsyncHttpClient/async-http-client/issues/364
    // http://stackoverflow.com/questions/18021722/ssl-and-http-proxy-with-java-client-issues-connectionclosedexception
    // Better to not set this and let people pass in a pre-configured builder instead.
    // asyncHttpConfigBuilder.setAllowSslConnectionPool(false)

    val sslEngineFactory = new DefaultSSLEngineFactory(sslConfig, sslContext, enabledProtocols = protocols, enabledCipherSuites = cipherSuites)

    // Hostname Processing
    val disableHostnameVerification = sslConfig.loose.flatMap(_.disableHostnameVerification).getOrElse(false)
    if (!disableHostnameVerification) {
      val hostnameVerifier = buildHostnameVerifier(sslConfig)
      builder.setHostnameVerifier(hostnameVerifier)
    } else {
      logger.debug("buildHostnameVerifier: disabling hostname verification")
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
}


/**
 * An SSL engine factory.  This factory is configured to make 1.6 more secure, but ideally you should use 1.7 JSSE as
 * the default settings are much better.  Notably, the cipher list in 1.7 is much improved and Server Name Indication
 * is enabled by default.
 *
 * Factory that creates an {@link SSLEngine} to be used for a single SSL connection.
 *
 * @see <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html">1.6 JSSE Reference Guide</a>
 * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html">1.7 JSSE Reference Guide</a>
 * @see <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider">1.6 SunJSSE Provider</a>
 * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider">1.7 SunJSSE Provider</a>
 */
class DefaultSSLEngineFactory(config: SSLConfig,
                              sslContext: SSLContext,
                              enabledProtocols: Array[String],
                              enabledCipherSuites: Array[String]) extends SSLEngineFactory {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Generates a new SSL engine with all the security settings defined.
   */
  def newSSLEngine(): SSLEngine = {
    logger.debug("newSSLEngine: started")

    val sslEngine = sslContext.createSSLEngine()
    sslEngine.setSSLParameters(sslContext.getDefaultSSLParameters)
    sslEngine.setEnabledProtocols(enabledProtocols)
    sslEngine.setEnabledCipherSuites(enabledCipherSuites)
    sslEngine.setUseClientMode(true)
    sslEngine
  }

  //
  //  def createSSLEngine(peerHost: String, peerPort: Int) = {
  //    // "Applications using this factory method are providing hints
  //    // for an internal session reuse strategy."
  //    // but the reason we need it here is because we get  "fatal error: 80: problem unwrapping net record" and the
  //    // following exception if we set sslParams.setEndpointIdentificationAlgorithm("HTTPS") and don't set host/port:
  //    //
  //    // [error] Caused by java.lang.NullPointerException: null
  //    // [error] sun.net.util.IPAddressUtil.textToNumericFormatV4(IPAddressUtil.java:42)
  //    // [error] sun.net.util.IPAddressUtil.isIPv4LiteralAddress(IPAddressUtil.java:255)
  //    // [error] sun.security.util.HostnameChecker.isIpAddress(HostnameChecker.java:122)
  //    // [error] sun.security.util.HostnameChecker.match(HostnameChecker.java:90)
  //    //
  //    // See http://stackoverflow.com/questions/13390964/java-ssl-fatal-error-80-unwrapping-net-record-after-adding-the-https-en
  //    // See http://stackoverflow.com/questions/13315623/netty-ssl-hostname-verification-support#comment18294294_13316257
  //    sslContext.createSSLEngine(peerHost, peerPort)
  //  }
  //
  //  /**
  //   * This method is not functional in 1.7 and is provided here for reference.
  //   */
  //  private def enableHostNameVerification(sslParams: SSLParameters): SSLEngine = {
  //    // The 1.6 code seems to work fine.  The 1.7 code will throw an NPE unless the engine is created with a hostname
  //    // and a port, which will cause problems if there's a single client being used against multiple hosts.
  //    useInternalHostnameVerification(sslParams)
  //  }
  //
  //  def useInternalHostnameVerification(sslParams: SSLParameters): SSLEngine = {
  //    Defaults.foldVersion(run16 = {
  //      val sslEngine = createSSLEngine
  //      if (!enableHostnameWith16(sslEngine)) {
  //        throw new IllegalStateException("Hostname verification failed!")
  //      }
  //      sslEngine
  //    }, runHigher = {
  //      val targetHost = peerHost.get
  //      val targetPort = peerPort.get
  //
  //      val sslEngine = createSSLEngine(targetHost, targetPort)
  //      if (!enableHostnameWith17(sslParams)) {
  //        throw new IllegalStateException("Hostname verification failed!")
  //      }
  //      sslEngine
  //    })
  //  }
  //
  //  def enableHostnameWith16(sslEngine: SSLEngine): Boolean = {
  //    // IN JDK 1.6, this is defined in sun.security.ssl.SSLEngineImpl.  Setting this to "HTTPS" enables
  //    // Handshaker to call X509ExtendedTrustManagerImpl.checkServerTrusted with "HTTPS" as the algorithm,
  //    // which then goes to sun.security.util.HostnameChecker.
  //    // Set the SSL options to "ssl trustmanager" to verify this.
  //    //
  //    // See http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames
  //
  //    try {
  //      val c = sslEngine.getClass
  //      val m = c.getMethod("trySetHostnameVerification", classOf[java.lang.String])
  //      m.invoke(sslEngine, "HTTPS": java.lang.String).asInstanceOf[java.lang.Boolean]
  //    } catch {
  //      case e: NoSuchMethodException =>
  //        logger.error("Method not found ", e)
  //        false
  //    }
  //  }
  //
  //  def enableHostnameWith17(sslParams: SSLParameters): Boolean = {
  //    try {
  //      val c = sslParams.getClass
  //      val m = c.getMethod("setEndpointIdentificationAlgorithm", classOf[java.lang.String])
  //      m.invoke(sslParams, "HTTPS": java.lang.String) // returns void
  //      true
  //    } catch {
  //      case e: NoSuchMethodException =>
  //        logger.error("Method not found ", e)
  //        false
  //    }
  //  }
  //
  //
  //  // This method is not enabled: it's at least clear we need access to the trust manager and a trust store, but
  //  // still not certain what this actually does...
  //  private def enable17Revocation(pkixParameters: java.security.cert.PKIXParameters) {
  //    logger.debug("enable17Revocation: using 1.7 params.setRevocationEnabled method")
  //    try {
  //      val c = pkixParameters.getClass
  //      val m = c.getMethod("setRevocationEnabled", classOf[java.lang.Boolean])
  //      m.invoke(pkixParameters, true: java.lang.Boolean).asInstanceOf[java.lang.Boolean]
  //    } catch {
  //      case e: NoSuchMethodException =>
  //        logger.error("enable17Revocation: Method not found ", e)
  //    }
  //  }

}

