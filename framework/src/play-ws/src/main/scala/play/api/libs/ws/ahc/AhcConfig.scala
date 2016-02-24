/*
 *
 *  * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 *
 */
package play.api.libs.ws.ahc

import java.security.KeyStore
import java.security.cert.CertPathValidatorException
import javax.inject.{ Singleton, Inject, Provider }

import org.asynchttpclient.netty.ssl.JsseSslEngineFactory
import org.slf4j.LoggerFactory

import org.asynchttpclient.{ DefaultAsyncHttpClientConfig, AsyncHttpClientConfig }

import javax.net.ssl._
import play.api.{ ConfigLoader, PlayConfig, Environment, Configuration }
import play.api.libs.ws.ssl._
import play.api.libs.ws.WSClientConfig

import scala.concurrent.duration._

/**
 * Ahc client config.
 *
 * @param wsClientConfig The general WS client config.
 * @param maxConnectionsPerHost The maximum number of connections to make per host. -1 means no maximum.
 * @param maxConnectionsTotal The maximum total number of connections. -1 means no maximum.
 * @param maxConnectionLifetime The maximum time that a connection should live for in the pool.
 * @param idleConnectionInPoolTimeout The time after which a connection that has been idle in the pool should be closed.
 * @param maxNumberOfRedirects The maximum number of redirects.
 * @param maxRequestRetry The maximum number of times to retry a request if it fails.
 * @param disableUrlEncoding Whether the raw URL should be used.
 * @param keepAlive keeps thread pool active, replaces allowPoolingConnection and allowSslConnectionPool
 */
case class AhcWSClientConfig(wsClientConfig: WSClientConfig = WSClientConfig(),
  maxConnectionsPerHost: Int = -1,
  maxConnectionsTotal: Int = -1,
  maxConnectionLifetime: Duration = Duration.Inf,
  idleConnectionInPoolTimeout: Duration = 1.minute,
  maxNumberOfRedirects: Int = 5,
  maxRequestRetry: Int = 5,
  disableUrlEncoding: Boolean = false,
  keepAlive: Boolean = true)

/**
 * Factory for creating AhcWSClientConfig, for use from Java.
 */
object AhcWSClientConfigFactory {

  def forClientConfig(config: WSClientConfig) = {
    AhcWSClientConfig(wsClientConfig = config)
  }
}

/**
 * This class creates a DefaultWSClientConfig object from the play.api.Configuration.
 */
@Singleton
class AhcWSClientConfigParser @Inject() (wsClientConfig: WSClientConfig,
    configuration: Configuration,
    environment: Environment) extends Provider[AhcWSClientConfig] {

  def get = parse()

  def parse(): AhcWSClientConfig = {

    val playConfig = PlayConfig(configuration)
    def get[A: ConfigLoader](name: String): A =
      playConfig.getDeprecated[A](s"play.ws.ahc.$name", s"play.ws.ning.$name")

    val maximumConnectionsPerHost = get[Int]("maxConnectionsPerHost")
    val maximumConnectionsTotal = get[Int]("maxConnectionsTotal")
    val maxConnectionLifetime = get[Duration]("maxConnectionLifetime")
    val idleConnectionInPoolTimeout = get[Duration]("idleConnectionInPoolTimeout")
    val maximumNumberOfRedirects = get[Int]("maxNumberOfRedirects")
    val maxRequestRetry = get[Int]("maxRequestRetry")
    val disableUrlEncoding = get[Boolean]("disableUrlEncoding")
    val keepAlive = get[Boolean]("keepAlive")

    // allowPoolingConnection and allowSslConnectionPool were merged into keepAlive in AHC 2.0
    // We want one value, keepAlive, and we don't want to confuse anyone who has to migrate.
    // keepAlive
    if (playConfig.underlying.hasPath("play.ws.ahc.keepAlive")) {
      val msg = "Both allowPoolingConnection and allowSslConnectionPool have been replaced by keepAlive!"
      Seq("play.ws.ning.allowPoolingConnection", "play.ws.ning.allowSslConnectionPool").foreach { s =>
        if (playConfig.underlying.hasPath(s)) {
          throw playConfig.reportError(s, msg)
        }
      }
    }
    AhcWSClientConfig(
      wsClientConfig = wsClientConfig,
      maxConnectionsPerHost = maximumConnectionsPerHost,
      maxConnectionsTotal = maximumConnectionsTotal,
      maxConnectionLifetime = maxConnectionLifetime,
      idleConnectionInPoolTimeout = idleConnectionInPoolTimeout,
      maxNumberOfRedirects = maximumNumberOfRedirects,
      maxRequestRetry = maxRequestRetry,
      disableUrlEncoding = disableUrlEncoding,
      keepAlive = keepAlive
    )
  }
}

/**
 * Builds a valid AsyncHttpClientConfig object from config.
 *
 * @param ahcConfig the ahc client configuration.
 */
class AhcConfigBuilder(ahcConfig: AhcWSClientConfig = AhcWSClientConfig()) {

  /**
   * Constructor for backwards compatibility with <= 2.3.X
   */
  @deprecated("Use AhcConfigBuilder(AhcWSClientConfig)", "2.5")
  def this(config: WSClientConfig) =
    this(AhcWSClientConfig(wsClientConfig = config))

  protected val addCustomSettings: DefaultAsyncHttpClientConfig.Builder => DefaultAsyncHttpClientConfig.Builder = identity

  /**
   * The underlying `DefaultAsyncHttpClientConfig.Builder` used by this instance.
   */
  val builder: DefaultAsyncHttpClientConfig.Builder = new DefaultAsyncHttpClientConfig.Builder()

  private[ahc] val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Configure the underlying builder with values specified by the `config`, and add any custom settings.
   *
   * @return the resulting builder
   */
  def configure(): DefaultAsyncHttpClientConfig.Builder = {
    val config = ahcConfig.wsClientConfig

    configureWS(ahcConfig)

    configureSSL(config.ssl)

    addCustomSettings(builder)
  }

  /**
   * Configure and build the `AsyncHttpClientConfig` based on the settings provided
   *
   * @return the resulting builder
   */
  def build(): AsyncHttpClientConfig = {
    configure().build()
  }

  /**
   * Modify the underlying `DefaultAsyncHttpClientConfig.Builder` using the provided function, after defaults are set.
   *
   * @param modify function with custom settings to apply to this builder before the client is built
   * @return the new builder
   */
  def modifyUnderlying(
    modify: DefaultAsyncHttpClientConfig.Builder => DefaultAsyncHttpClientConfig.Builder): AhcConfigBuilder = {
    new AhcConfigBuilder(ahcConfig) {
      override val addCustomSettings = modify compose AhcConfigBuilder.this.addCustomSettings
      override val builder = AhcConfigBuilder.this.builder
    }
  }

  /**
   * Configures the global settings.
   */
  def configureWS(ahcConfig: AhcWSClientConfig): Unit = {
    val config = ahcConfig.wsClientConfig

    def toMillis(duration: Duration): Int = {
      if (duration.isFinite()) duration.toMillis.toInt
      else -1
    }

    builder.setConnectTimeout(toMillis(config.connectionTimeout))
      .setReadTimeout(toMillis(config.idleTimeout))
      .setRequestTimeout(toMillis(config.requestTimeout))
      .setFollowRedirect(config.followRedirects)
      .setUseProxyProperties(config.useProxyProperties)
      .setCompressionEnforced(config.compressionEnabled)

    config.userAgent foreach builder.setUserAgent

    builder.setMaxConnectionsPerHost(ahcConfig.maxConnectionsPerHost)
    builder.setMaxConnections(ahcConfig.maxConnectionsTotal)
    builder.setConnectionTtl(toMillis(ahcConfig.maxConnectionLifetime))
    builder.setPooledConnectionIdleTimeout(toMillis(ahcConfig.idleConnectionInPoolTimeout))
    builder.setMaxRedirects(ahcConfig.maxNumberOfRedirects)
    builder.setMaxRequestRetry(ahcConfig.maxRequestRetry)
    builder.setDisableUrlEncodingForBoundRequests(ahcConfig.disableUrlEncoding)
    builder.setKeepAlive(ahcConfig.keepAlive)
    // forcing shutdown of the AHC event loop because otherwise the test suite fails with a
    // OutOfMemoryException: cannot create new native thread. This is because when executing
    // tests in parallel there can be many threads pool that are left around because AHC is
    // shutting them down gracefully.
    // The proper solution is to make these parameters configurable, so that they can be set
    // to 0 when running tests, and keep sensible defaults otherwise. AHC defaults are
    // shutdownQuiet=2000 (milliseconds) and shutdownTimeout=15000 (milliseconds).
    builder.setShutdownQuietPeriod(0)
    builder.setShutdownTimeout(0)
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

    if (!sslConfig.loose.allowWeakProtocols) {
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

    if (!sslConfig.loose.allowWeakCiphers) {
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
    val sslContext = if (sslConfig.default) {
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
    builder.setEnabledProtocols(protocols)

    // ciphers!
    val defaultCiphers = defaultParams.getCipherSuites
    val cipherSuites = configureCipherSuites(defaultCiphers, sslConfig)
    defaultParams.setCipherSuites(cipherSuites)
    builder.setEnabledCipherSuites(cipherSuites)

    builder.setAcceptAnyCertificate(sslConfig.loose.acceptAnyCertificate)

    builder.setSslEngineFactory(new JsseSslEngineFactory(sslContext))
  }

  def buildKeyManagerFactory(ssl: SSLConfig): KeyManagerFactoryWrapper = {
    new DefaultKeyManagerFactoryWrapper(ssl.keyManagerConfig.algorithm)
  }

  def buildTrustManagerFactory(ssl: SSLConfig): TrustManagerFactoryWrapper = {
    new DefaultTrustManagerFactoryWrapper(ssl.trustManagerConfig.algorithm)
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

    val constraints = sslConfig.disabledKeyAlgorithms.map(a => AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.expression, a).get).toSet
    val algorithmChecker = new AlgorithmChecker(keyConstraints = constraints, signatureConstraints = Set())
    for (cert <- trustManager.getAcceptedIssuers) {
      try {
        algorithmChecker.checkKeyAlgorithms(cert)
      } catch {
        case e: CertPathValidatorException =>
          logger.warn("You are using ws.ssl.default=true and have a weak certificate in your default trust store!  (You can modify ws.ssl.disabledKeyAlgorithms to remove this message.)", e)
      }
    }
  }
}
