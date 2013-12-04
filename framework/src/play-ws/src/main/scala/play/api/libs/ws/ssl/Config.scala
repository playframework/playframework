/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl


import play.api.{Logger, Configuration}
import scala.collection.JavaConverters
import java.security.SecureRandom

/**
 * Contains configuration information for a key store.
 */
trait KeyStoreConfig {

  def storeType: Option[String]

  def filePath: Option[String]

  def data: Option[String]

  def password: Option[String]
}

/**
 * Contains configuration information for a trust store.
 */
trait TrustStoreConfig {

  def storeType: Option[String]

  def filePath: Option[String]

  def data: Option[String]
}

/**
 * Contains configuration information for a key manager.
 */
trait KeyManagerConfig {

  def algorithm: Option[String]

  def keyStoreConfigs: Seq[KeyStoreConfig]

  def password: Option[String]
}

/**
 * Contains configuration information for a trust manager.
 */
trait TrustManagerConfig {

  def algorithm: Option[String]

  def trustStoreConfigs: Seq[TrustStoreConfig]
}

/**
 * Contains information for configuring a JSSE SSL context.
 */
trait SSLConfig {

  def off: Option[Boolean]

  def default: Option[Boolean]

  def protocol: Option[String]

  def secureRandom: Option[SecureRandom]

  def hostnameVerifierClassName: Option[String]

  def enabledCipherSuites: Option[Seq[String]]

  def enabledProtocols: Option[Seq[String]]

  def disabledAlgorithms: Option[String]

  def keyManagerConfig: Option[KeyManagerConfig]

  def trustManagerConfig: Option[TrustManagerConfig]

  def debug: Option[SSLDebugConfig]

  def loose: Option[SSLLooseConfig]
}

trait SSLLooseConfig {

  /**
   * Disables OCSP revocation checks.
   */
  def disableCheckRevocation: Option[Boolean]

  // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
  def allowLegacyHelloMessages: Option[Boolean]

  // @see http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
  def allowUnsafeRenegotiation: Option[Boolean]

  def allowWeakCiphers: Option[Boolean]

  def allowWeakProtocols: Option[Boolean]

  def disableHostnameVerification: Option[Boolean]

}

// Key Store implementation

case class DefaultKeyStoreConfig(storeType: Option[String],
                                 filePath: Option[String],
                                 data: Option[String],
                                 password: Option[String]) extends KeyStoreConfig

// Trust Store implementation

case class DefaultTrustStoreConfig(storeType: Option[String],
                                   filePath: Option[String],
                                   data: Option[String]) extends TrustStoreConfig

// Managers and context

case class DefaultKeyManagerConfig(algorithm: Option[String] = None,
                                   keyStoreConfigs: Seq[KeyStoreConfig] = Nil,
                                   password: Option[String] = None) extends KeyManagerConfig

case class DefaultTrustManagerConfig(algorithm: Option[String] = None,
                                     trustStoreConfigs: Seq[TrustStoreConfig] = Nil) extends TrustManagerConfig


case class SSLDebugConfig(all: Boolean = false,
                          ssl: Boolean = false,
                          certpath: Boolean = false,
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

  def withAll = this.copy(all = true)

  def withCertPath = this.copy(certpath = true)

  /**
   * -Djavax.ssl.debug=handshake
   */
  def withRecord(plaintext: Boolean = false, packet: Boolean = false) = {
    this.copy(record = Some(SSLDebugRecordOptions(plaintext, packet)))
  }

  /**
   * -Djavax.ssl.debug=handshake
   */
  def withHandshake(data: Boolean = false, verbose: Boolean = false) = {
    this.copy(handshake = Some(SSLDebugHandshakeOptions(data, verbose)))
  }

  def withSSL = this.copy(ssl = true)

  /**
   * -Djavax.ssl.debug=keygen
   */
  def withKeygen = this.copy(keygen = true)

  /**
   * -Djavax.ssl.debug=session
   */
  def withSession = this.copy(session = true)

  /**
   * -Djavax.ssl.debug=defaultctx
   */
  def withDefaultContext = this.copy(defaultctx = true)

  /**
   * -Djavax.ssl.debug=sslctx
   */
  def withSSLContext = this.copy(sslctx = true)

  /**
   * -Djavax.ssl.debug=sessioncache
   */
  def withSessionCache = this.copy(sessioncache = true)

  /**
   * -Djavax.ssl.debug=keymanager
   */
  def withKeyManager = this.copy(keymanager = true)

  /**
   * -Djavax.ssl.debug=trustmanager
   */
  def withTrustManager = this.copy(trustmanager = true)

  /**
   * -Djavax.ssl.debug=pluggability
   */
  def withPluggability = this.copy(pluggability = true)

}

case class SSLDebugHandshakeOptions(data: Boolean = false, verbose: Boolean = false)

case class SSLDebugRecordOptions(plaintext: Boolean = false, packet: Boolean = false)

case class DefaultSSLLooseConfig(allowWeakCiphers: Option[Boolean] = None,
                                 allowWeakProtocols: Option[Boolean] = None,
                                 allowLegacyHelloMessages: Option[Boolean] = None,
                                 allowUnsafeRenegotiation: Option[Boolean] = None,
                                 disableCheckRevocation: Option[Boolean] = None,
                                 disableHostnameVerification: Option[Boolean] = None) extends SSLLooseConfig

case class DefaultSSLConfig(off: Option[Boolean] = None,
                            default: Option[Boolean] = None,
                            protocol: Option[String] = None,
                            enabledCipherSuites: Option[Seq[String]] = None,
                            enabledProtocols: Option[Seq[String]] = None,
                            disabledAlgorithms: Option[String] = None,
                            keyManagerConfig: Option[KeyManagerConfig] = None,
                            trustManagerConfig: Option[TrustManagerConfig] = None,
                            hostnameVerifierClassName: Option[String] = None,
                            secureRandom: Option[SecureRandom] = None,
                            debug: Option[SSLDebugConfig] = None,
                            loose: Option[SSLLooseConfig] = None) extends SSLConfig




trait SSLConfigParser {
  def parse(): SSLConfig
}

class DefaultSSLConfigParser(c: Configuration) {

  def parse(): SSLConfig = {
    val off = c.getBoolean("off")

    val default = c.getBoolean("default")

    val algorithm = c.getString("algorithm")

    val debug = c.getStringSeq("debug").map {
      debugConfig =>
        parseDebug(debugConfig)
    }

    val looseOptions = c.getConfig("loose").map {
      looseConfig => parseLooseOptions(looseConfig)
    }

    val ciphers = c.getStringSeq("enabledCipherSuites")

    val protocols = c.getStringSeq("enabledProtocols")

    val hostnameVerifierClassName = c.getString("hostnameVerifierClassName")

    val disabledAlgorithms = c.getString("disabledAlgorithms")

    val keyManagers: Option[KeyManagerConfig] = c.getConfig("keyManager").map {
      keyManagerConfig =>
        parseKeyManager(keyManagerConfig)
    }

    val trustManagers: Option[TrustManagerConfig] = c.getConfig("trustManager").map {
      trustStoreConfig =>
        parseTrustManager(trustStoreConfig)
    }

    DefaultSSLConfig(
      off = off,
      default = default,
      protocol = algorithm,
      enabledCipherSuites = ciphers,
      enabledProtocols = protocols,
      keyManagerConfig = keyManagers,
      hostnameVerifierClassName = hostnameVerifierClassName,
      disabledAlgorithms = disabledAlgorithms,
      trustManagerConfig = trustManagers,
      secureRandom = None,
      debug = debug,
      loose = looseOptions)
  }

  /**
   * Parses "ws.ssl.loose" section.
   */
  def parseLooseOptions(looseOptions: Configuration): SSLLooseConfig = {

    val allowMessages: Option[Boolean] = looseOptions.getBoolean("allowLegacyHelloMessages")

    val allowWeakProtocols = looseOptions.getBoolean("allowWeakProtocols")

    val allowWeakCiphers = looseOptions.getBoolean("allowWeakCiphers")

    val allowUnsafeRenegotiation = looseOptions.getBoolean("allowUnsafeRenegotiation")

    val disableCheckRevocation = looseOptions.getBoolean("disableCheckRevocation")

    val disableHostnameVerification = looseOptions.getBoolean("disableHostnameVerification")

    DefaultSSLLooseConfig(
      allowWeakCiphers = allowWeakCiphers,
      allowWeakProtocols = allowWeakProtocols,
      allowLegacyHelloMessages = allowMessages,
      allowUnsafeRenegotiation = allowUnsafeRenegotiation,
      disableCheckRevocation = disableCheckRevocation,
      disableHostnameVerification = disableHostnameVerification
    )
  }

  /**
   * Parses the "ws.ssl.debug" section.
   */
  def parseDebug(list: Seq[String]): SSLDebugConfig = {
    val certpath = list.contains("certpath")

    if (list.contains("all")) {
      SSLDebugConfig(all = true, certpath = certpath)
    } else {
      val record: Option[SSLDebugRecordOptions] = list.find(_ == "record").map {
        r =>
          val plaintext: Boolean = list.contains("plaintext")
          val packet: Boolean = list.contains("packet")
          SSLDebugRecordOptions(plaintext = plaintext, packet = packet)
      }

      val handshake = list.find(_ == "handshake").map {
        ho =>
          val data = list.contains("data")
          val verbose = list.contains("verbose")
          SSLDebugHandshakeOptions(data = data, verbose = verbose)
      }

      val keygen = list.contains("keygen")

      val session = list.contains("session")

      val defaultctx = list.contains("defaultctx")

      val sslctx = list.contains("sslctx")

      val sessioncache = list.contains("sessioncache")

      val keymanager = list.contains("keymanager")

      val trustmanager = list.contains("trustmanager")

      val pluggability = list.contains("pluggability")

      SSLDebugConfig(record = record,
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
  def parseKeyStoreInfo(config: Configuration): KeyStoreConfig = {
    val storeType = config.getString("type")
    val path = config.getString("path")
    val password = config.getString("password")
    val data = config.getString("data")

    if (data.isEmpty && path.isEmpty) {
      throw new IllegalStateException("You must specify either 'path' or 'data' in ws.ssl.keyManager.stores list!")
    }

    DefaultKeyStoreConfig(filePath = path, storeType = storeType, data = data, password = password)
  }

  /**
   * Parses the "ws.ssl.trustManager { stores = [ ... ]" section of configuration.
   */
  def parseTrustStoreInfo(config: Configuration): TrustStoreConfig = {
    val storeType = config.getString("type")
    val path = config.getString("path")
    val data = config.getString("data")

    if (data.isEmpty && path.isEmpty) {
      throw new IllegalStateException("You must specify either 'path' or 'data' in ws.ssl.trustManagers.stores list!")
    }

    DefaultTrustStoreConfig(filePath = path, storeType = storeType, data = data)
  }

  /**
   * Parses the "ws.ssl.keyManager" section of the configuration.
   */
  def parseKeyManager(config: Configuration): KeyManagerConfig = {

    val algorithm = config.getString("algorithm")

    val password = config.getString("password")

    val keyStoreInfos = config.getObjectList("stores").map {
      storeList =>
      // storeList is a java.util.List, not a scala.List
        import JavaConverters._
        for {
          store <- storeList.asScala
        } yield {
          val storeConfig = Configuration(store.toConfig)
          parseKeyStoreInfo(storeConfig)
        }
    }.getOrElse(Nil)

    DefaultKeyManagerConfig(algorithm, keyStoreInfos, password)
  }

  /**
   * Parses the "ws.ssl.trustManager" section of configuration.
   */

  def parseTrustManager(config: Configuration): TrustManagerConfig = {
    val algorithm: Option[String] = config.getString("algorithm")

    val trustStoreInfos = config.getObjectList("stores").map {
      storeList =>
      // storeList is a java.util.List, not a scala.List
        import JavaConverters._
        for {
          store <- storeList.asScala
        } yield {
          val storeConfig = Configuration(store.toConfig)
          parseTrustStoreInfo(storeConfig)
        }
    }.getOrElse(Nil)

    DefaultTrustManagerConfig(algorithm, trustStoreInfos)
  }
}
