/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.PlayConfig
import java.security.{ KeyStore, SecureRandom }
import java.net.URL
import javax.net.ssl.{ TrustManagerFactory, KeyManagerFactory, HostnameVerifier }

/**
 * Configuration for a keystore.
 *
 * A key store must either provide a file path, or a data String.
 *
 * @param storeType The store type. Defaults to the platform default store type (ie, JKS).
 * @param filePath The path of the key store file.
 * @param data The data to load the key store file from.
 * @param password The password to use to load the key store file, if the file is password protected.
 */
case class KeyStoreConfig(storeType: String = KeyStore.getDefaultType,
    filePath: Option[String] = None,
    data: Option[String] = None,
    password: Option[String] = None) {

  assert(filePath.isDefined ^ data.isDefined, "Either key store path or data must be defined, but not both.")
}

/**
 * Configuration for a trust store.
 *
 * A trust store must either provide a file path, or a data String.
 *
 * @param storeType The store type. Defaults to the platform default store type (ie, JKS).
 * @param filePath The path of the key store file.
 * @param data The data to load the key store file from.
 */
case class TrustStoreConfig(storeType: String = KeyStore.getDefaultType,
    filePath: Option[String],
    data: Option[String]) {

  assert(filePath.isDefined ^ data.isDefined, "Either trust store path or data must be defined, but not both.")
}

/**
 * The key manager config.
 *
 * @param algorithm The algoritm to use.
 * @param keyStoreConfigs The key stores to use.
 */
case class KeyManagerConfig(
  algorithm: String = KeyManagerFactory.getDefaultAlgorithm,
  keyStoreConfigs: Seq[KeyStoreConfig] = Nil)

/**
 * The trust manager config.
 *
 * @param algorithm The algorithm to use.
 * @param trustStoreConfigs The trust stores to use.
 */
case class TrustManagerConfig(
  algorithm: String = TrustManagerFactory.getDefaultAlgorithm,
  trustStoreConfigs: Seq[TrustStoreConfig] = Nil)

/**
 * SSL debug configuration.
 */
case class SSLDebugConfig(
    all: Boolean = false,
    ssl: Boolean = false,
    certpath: Boolean = false,
    ocsp: Boolean = false,
    record: Option[SSLDebugRecordOptions] = None,
    handshake: Option[SSLDebugHandshakeOptions] = None,
    keygen: Boolean = false,
    session: Boolean = false,
    defaultctx: Boolean = false,
    sslctx: Boolean = false,
    sessioncache: Boolean = false,
    keymanager: Boolean = false,
    trustmanager: Boolean = false,
    pluggability: Boolean = false) {

  /**
   * Whether any debug options are enabled.
   */
  def enabled = all || ssl || certpath || ocsp || record.isDefined || handshake.isDefined ||
    keygen || session || defaultctx || sslctx || sessioncache || keymanager || trustmanager ||
    pluggability

  def withAll = this.copy(all = true)

  def withCertPath = this.copy(certpath = true)

  def withOcsp = this.withCertPath.copy(ocsp = true) // technically a part of certpath, only available in 1.7+

  def withRecord(plaintext: Boolean = false, packet: Boolean = false) = {
    this.copy(record = Some(SSLDebugRecordOptions(plaintext, packet)))
  }

  def withHandshake(data: Boolean = false, verbose: Boolean = false) = {
    this.copy(handshake = Some(SSLDebugHandshakeOptions(data, verbose)))
  }

  def withSSL = this.copy(ssl = true)

  def withKeygen = this.copy(keygen = true)

  def withSession = this.copy(session = true)

  def withDefaultContext = this.copy(defaultctx = true)

  def withSSLContext = this.copy(sslctx = true)

  def withSessionCache = this.copy(sessioncache = true)

  def withKeyManager = this.copy(keymanager = true)

  def withTrustManager = this.copy(trustmanager = true)

  def withPluggability = this.copy(pluggability = true)

}

/**
 * SSL handshake debugging options.
 */
case class SSLDebugHandshakeOptions(data: Boolean = false, verbose: Boolean = false)

/**
 * SSL record debugging options.
 */
case class SSLDebugRecordOptions(plaintext: Boolean = false, packet: Boolean = false)

/**
 * Configuration for specifying loose (potentially dangerous) ssl config.
 *
 * @param allowWeakCiphers Whether weak ciphers should be allowed or not.
 * @param allowWeakProtocols Whether weak protocols should be allowed or not.
 * @param allowLegacyHelloMessages Whether legacy hello messages should be allowed or not.  If None, uses the platform
 *                                 default.
 * @param allowUnsafeRenegotiation Whether unsafe renegotiation should be allowed or not. If None, uses the platform
 *                                 default.
 * @param disableHostnameVerification Whether hostname verification should be disabled.
 * @param acceptAnyCertificate Whether any X.509 certificate should be accepted or not.
 */
case class SSLLooseConfig(
  allowWeakCiphers: Boolean = false,
  allowWeakProtocols: Boolean = false,
  allowLegacyHelloMessages: Option[Boolean] = None,
  allowUnsafeRenegotiation: Option[Boolean] = None,
  disableHostnameVerification: Boolean = false,
  acceptAnyCertificate: Boolean = false)

/**
 * The SSL configuration.
 *
 * @param default Whether we should use the default JVM SSL configuration or not.
 * @param protocol The SSL protocol to use. Defaults to TLSv1.2.
 * @param checkRevocation Whether revocation lists should be checked, if None, defaults to platform default setting.
 * @param revocationLists The revocation lists to check.
 * @param enabledCipherSuites If defined, override the platform default cipher suites.
 * @param enabledProtocols If defined, override the platform default protocols.
 * @param disabledSignatureAlgorithms The disabled signature algorithms.
 * @param disabledKeyAlgorithms The disabled key algorithms.
 * @param keyManagerConfig The key manager configuration.
 * @param trustManagerConfig The trust manager configuration.
 * @param hostnameVerifierClass The hostname verifier class.
 * @param secureRandom The SecureRandom instance to use. Let the platform choose if None.
 * @param debug The debug config.
 * @param loose Loose configuratino parameters
 */
case class SSLConfig(
  default: Boolean = false,
  protocol: String = "TLSv1.2",
  checkRevocation: Option[Boolean] = None,
  revocationLists: Option[Seq[URL]] = None,
  enabledCipherSuites: Option[Seq[String]] = None,
  enabledProtocols: Option[Seq[String]] = Some(Seq("TLSv1.2", "TLSv1.1", "TLSv1")),
  disabledSignatureAlgorithms: Seq[String] = Seq("MD2", "MD4", "MD5"),
  disabledKeyAlgorithms: Seq[String] = Seq("RSA keySize < 2048", "DSA keySize < 2048", "EC keySize < 224"),
  keyManagerConfig: KeyManagerConfig = KeyManagerConfig(),
  trustManagerConfig: TrustManagerConfig = TrustManagerConfig(),
  hostnameVerifierClass: Class[_ <: HostnameVerifier] = classOf[DefaultHostnameVerifier],
  secureRandom: Option[SecureRandom] = None,
  debug: SSLDebugConfig = SSLDebugConfig(),
  loose: SSLLooseConfig = SSLLooseConfig())

/**
 * Factory for creating SSL config (for use from Java).
 */
object SSLConfigFactory {

  /**
   * Create an instance of the default config
   * @return
   */
  def defaultConfig = SSLConfig()
}

class SSLConfigParser(c: PlayConfig, classLoader: ClassLoader) {

  def parse(): SSLConfig = {

    val default = c.get[Boolean]("default")
    val protocol = c.get[String]("protocol")
    val checkRevocation = c.getOptional[Boolean]("checkRevocation")
    val revocationLists: Option[Seq[URL]] = Some(
      c.get[Seq[String]]("revocationLists").map(new URL(_))
    ).filter(_.nonEmpty)

    val debug = parseDebug(c.get[PlayConfig]("debug"))
    val looseOptions = parseLooseOptions(c.get[PlayConfig]("loose"))

    val ciphers = Some(c.get[Seq[String]]("enabledCipherSuites")).filter(_.nonEmpty)
    val protocols = Some(c.get[Seq[String]]("enabledProtocols")).filter(_.nonEmpty)

    val hostnameVerifierClass = c.getOptional[String]("hostnameVerifierClass") match {
      case None => classOf[DefaultHostnameVerifier]
      case Some(fqcn) => classLoader.loadClass(fqcn).asSubclass(classOf[HostnameVerifier])
    }

    val disabledSignatureAlgorithms = c.get[Seq[String]]("disabledSignatureAlgorithms")
    val disabledKeyAlgorithms = c.get[Seq[String]]("disabledKeyAlgorithms")

    val keyManagers = parseKeyManager(c.get[PlayConfig]("keyManager"))

    val trustManagers = parseTrustManager(c.get[PlayConfig]("trustManager"))

    SSLConfig(
      default = default,
      protocol = protocol,
      checkRevocation = checkRevocation,
      revocationLists = revocationLists,
      enabledCipherSuites = ciphers,
      enabledProtocols = protocols,
      keyManagerConfig = keyManagers,
      hostnameVerifierClass = hostnameVerifierClass,
      disabledSignatureAlgorithms = disabledSignatureAlgorithms,
      disabledKeyAlgorithms = disabledKeyAlgorithms,
      trustManagerConfig = trustManagers,
      secureRandom = None,
      debug = debug,
      loose = looseOptions)
  }

  /**
   * Parses "ws.ssl.loose" section.
   */
  def parseLooseOptions(config: PlayConfig): SSLLooseConfig = {

    val allowWeakProtocols = config.get[Boolean]("allowWeakProtocols")
    val allowWeakCiphers = config.get[Boolean]("allowWeakCiphers")
    val allowMessages = config.getOptional[Boolean]("allowLegacyHelloMessages")
    val allowUnsafeRenegotiation = config.getOptional[Boolean]("allowUnsafeRenegotiation")
    val disableHostnameVerification = config.get[Boolean]("disableHostnameVerification")
    val acceptAnyCertificate = config.get[Boolean]("acceptAnyCertificate")

    SSLLooseConfig(
      allowWeakCiphers = allowWeakCiphers,
      allowWeakProtocols = allowWeakProtocols,
      allowLegacyHelloMessages = allowMessages,
      allowUnsafeRenegotiation = allowUnsafeRenegotiation,
      disableHostnameVerification = disableHostnameVerification,
      acceptAnyCertificate = acceptAnyCertificate
    )
  }

  /**
   * Parses the "ws.ssl.debug" section.
   */
  def parseDebug(config: PlayConfig): SSLDebugConfig = {
    val certpath = config.get[Boolean]("certpath")

    if (config.get[Boolean]("all")) {
      SSLDebugConfig(all = true, certpath = certpath)
    } else {

      val record: Option[SSLDebugRecordOptions] = if (config.get[Boolean]("record")) {
        val plaintext = config.get[Boolean]("plaintext")
        val packet = config.get[Boolean]("packet")
        Some(SSLDebugRecordOptions(plaintext = plaintext, packet = packet))
      } else None

      val handshake = if (config.get[Boolean]("handshake")) {
        val data = config.get[Boolean]("data")
        val verbose = config.get[Boolean]("verbose")
        Some(SSLDebugHandshakeOptions(data = data, verbose = verbose))
      } else {
        None
      }

      val keygen = config.get[Boolean]("keygen")
      val session = config.get[Boolean]("session")
      val defaultctx = config.get[Boolean]("defaultctx")
      val sslctx = config.get[Boolean]("sslctx")
      val sessioncache = config.get[Boolean]("sessioncache")
      val keymanager = config.get[Boolean]("keymanager")
      val trustmanager = config.get[Boolean]("trustmanager")
      val pluggability = config.get[Boolean]("pluggability")
      val ssl = config.get[Boolean]("ssl")

      SSLDebugConfig(
        ssl = ssl,
        record = record,
        handshake = handshake,
        keygen = keygen,
        session = session,
        defaultctx = defaultctx,
        sslctx = sslctx,
        sessioncache = sessioncache,
        keymanager = keymanager,
        trustmanager = trustmanager,
        pluggability = pluggability,
        certpath = certpath)
    }
  }

  /**
   * Parses the "ws.ssl.keyManager { stores = [ ... ]" section of configuration.
   */
  def parseKeyStoreInfo(config: PlayConfig): KeyStoreConfig = {
    val storeType = config.getOptional[String]("type").getOrElse(KeyStore.getDefaultType)
    val path = config.getOptional[String]("path")
    val data = config.getOptional[String]("data")
    val password = config.getOptional[String]("password")

    KeyStoreConfig(filePath = path, storeType = storeType, data = data, password = password)
  }

  /**
   * Parses the "ws.ssl.trustManager { stores = [ ... ]" section of configuration.
   */
  def parseTrustStoreInfo(config: PlayConfig): TrustStoreConfig = {
    val storeType = config.getOptional[String]("type").getOrElse(KeyStore.getDefaultType)
    val path = config.getOptional[String]("path")
    val data = config.getOptional[String]("data")

    TrustStoreConfig(filePath = path, storeType = storeType, data = data)
  }

  /**
   * Parses the "ws.ssl.keyManager" section of the configuration.
   */
  def parseKeyManager(config: PlayConfig): KeyManagerConfig = {

    val algorithm = config.getOptional[String]("algorithm") match {
      case None => KeyManagerFactory.getDefaultAlgorithm
      case Some(other) => other
    }

    val keyStoreInfos = config.getPrototypedSeq("stores").map { store =>
      parseKeyStoreInfo(store)
    }

    KeyManagerConfig(algorithm, keyStoreInfos)
  }

  /**
   * Parses the "ws.ssl.trustManager" section of configuration.
   */

  def parseTrustManager(config: PlayConfig): TrustManagerConfig = {
    val algorithm = config.getOptional[String]("algorithm") match {
      case None => TrustManagerFactory.getDefaultAlgorithm
      case Some(other) => other
    }

    val trustStoreInfos = config.getPrototypedSeq("stores").map { store =>
      parseTrustStoreInfo(store)
    }

    TrustManagerConfig(algorithm, trustStoreInfos)
  }
}
