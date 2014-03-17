/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import play.api.{ Logger, Configuration }
import scala.collection.JavaConverters
import java.security.SecureRandom
import java.net.URL

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

  def default: Option[Boolean]

  def protocol: Option[String]

  def checkRevocation: Option[Boolean]

  def revocationLists: Option[Seq[URL]]

  def secureRandom: Option[SecureRandom]

  def hostnameVerifierClassName: Option[String]

  def enabledCipherSuites: Option[Seq[String]]

  def enabledProtocols: Option[Seq[String]]

  def disabledSignatureAlgorithms: Option[String]

  def disabledKeyAlgorithms: Option[String]

  def keyManagerConfig: Option[KeyManagerConfig]

  def trustManagerConfig: Option[TrustManagerConfig]

  def debug: Option[SSLDebugConfig]

  def loose: Option[SSLLooseConfig]
}

trait SSLLooseConfig {

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

case class DefaultTrustManagerConfig(
  algorithm: Option[String] = None,
  trustStoreConfigs: Seq[TrustStoreConfig] = Nil) extends TrustManagerConfig

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

case class SSLDebugHandshakeOptions(data: Boolean = false, verbose: Boolean = false)

case class SSLDebugRecordOptions(plaintext: Boolean = false, packet: Boolean = false)

case class DefaultSSLLooseConfig(
  allowWeakCiphers: Option[Boolean] = None,
  allowWeakProtocols: Option[Boolean] = None,
  allowLegacyHelloMessages: Option[Boolean] = None,
  allowUnsafeRenegotiation: Option[Boolean] = None,
  disableHostnameVerification: Option[Boolean] = None) extends SSLLooseConfig

case class DefaultSSLConfig(
  default: Option[Boolean] = None,
  protocol: Option[String] = None,
  checkRevocation: Option[Boolean] = None,
  revocationLists: Option[Seq[URL]] = None,
  enabledCipherSuites: Option[Seq[String]] = None,
  enabledProtocols: Option[Seq[String]] = None,
  disabledSignatureAlgorithms: Option[String] = None,
  disabledKeyAlgorithms: Option[String] = None,
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

    val default = c.getBoolean("default")

    val protocol = c.getString("protocol")

    val checkRevocation = c.getBoolean("checkRevocation")

    val revocationLists: Option[Seq[URL]] = c.getStringSeq("revocationLists").map {
      urls =>
        for {
          revocationURL <- urls
        } yield {
          new URL(revocationURL)
        }
    }

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

    val disabledSignatureAlgorithms = c.getString("disabledSignatureAlgorithms")

    val disabledKeyAlgorithms = c.getString("disabledKeyAlgorithms")

    val keyManagers: Option[KeyManagerConfig] = c.getConfig("keyManager").map {
      keyManagerConfig =>
        parseKeyManager(keyManagerConfig)
    }

    val trustManagers: Option[TrustManagerConfig] = c.getConfig("trustManager").map {
      trustStoreConfig =>
        parseTrustManager(trustStoreConfig)
    }

    val secureRandom = new SecureRandom()
    // SecureRandom needs to be seeded, and calling nextInt immediately after being
    // called ensures a seed.  We do it here rather than waiting for JSSE because
    // seeding can chew through entropy and occasionally block the thread until
    // it's finished, if on something too dumb to use /dev/urandom.
    // Better to do it using parsing rather than in the middle of an HTTPS call.
    secureRandom.nextInt()

    DefaultSSLConfig(
      default = default,
      protocol = protocol,
      checkRevocation = checkRevocation,
      revocationLists = revocationLists,
      enabledCipherSuites = ciphers,
      enabledProtocols = protocols,
      keyManagerConfig = keyManagers,
      hostnameVerifierClassName = hostnameVerifierClassName,
      disabledSignatureAlgorithms = disabledSignatureAlgorithms,
      disabledKeyAlgorithms = disabledKeyAlgorithms,
      trustManagerConfig = trustManagers,
      secureRandom = Some(secureRandom),
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

    val disableHostnameVerification = looseOptions.getBoolean("disableHostnameVerification")

    DefaultSSLLooseConfig(
      allowWeakCiphers = allowWeakCiphers,
      allowWeakProtocols = allowWeakProtocols,
      allowLegacyHelloMessages = allowMessages,
      allowUnsafeRenegotiation = allowUnsafeRenegotiation,
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

      val ssl = list.contains("ssl")

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
