/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.net.ssl.{ SSLEngine, X509ExtendedKeyManager, X509KeyManager }
import java.security.{ Principal, PrivateKey }
import java.security.cert.{ CertificateException, X509Certificate }
import java.net.Socket
import scala.collection.mutable.ArrayBuffer

/**
 * A keymanager that wraps other X509 key managers.
 */
class CompositeX509KeyManager(keyManagers: Seq[X509KeyManager]) extends X509ExtendedKeyManager {
  // Must specify X509ExtendedKeyManager: otherwise you get
  // "X509KeyManager passed to SSLContext.init():  need an X509ExtendedKeyManager for SSLEngine use"

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  logger.debug(s"CompositeX509KeyManager start: keyManagers = $keyManagers")

  // You would think that from the method signature that you could use multiple key managers and trust managers
  // by passing them as an array in the init method.  However, the fine print explicitly says:
  // "Only the first instance of a particular key and/or trust manager implementation type in the array is used.
  // (For example, only the first javax.net.ssl.X509KeyManager in the array will be used.)"
  //
  // This doesn't mean you can't have multiple key managers, but that you can't have multiple key managers of
  // the same class, i.e. you can't have two X509KeyManagers in the array.

  def getClientAliases(keyType: String, issuers: Array[Principal]): Array[String] = {
    logger.debug(s"getClientAliases: keyType = $keyType, issuers = ${issuers.toSeq}")

    val clientAliases = new ArrayBuffer[String]
    withKeyManagers { keyManager =>
      val aliases = keyManager.getClientAliases(keyType, issuers)
      if (aliases != null) {
        clientAliases.appendAll(aliases)
      }
    }
    logger.debug(s"getCertificateChain: clientAliases = $clientAliases")

    nullIfEmpty(clientAliases.toArray)
  }

  def chooseClientAlias(keyType: Array[String], issuers: Array[Principal], socket: Socket): String = {
    logger.debug(s"chooseClientAlias: keyType = ${keyType.toSeq}, issuers = ${issuers.toSeq}, socket = $socket")

    withKeyManagers { keyManager =>
      val clientAlias = keyManager.chooseClientAlias(keyType, issuers, socket)
      if (clientAlias != null) {
        logger.debug(s"chooseClientAlias: using clientAlias $clientAlias with keyManager $keyManager")
        return clientAlias
      }
    }
    null
  }

  override def chooseEngineClientAlias(keyType: Array[String], issuers: Array[Principal], engine: SSLEngine): String = {
    logger.debug(s"chooseEngineClientAlias: keyType = ${keyType.toSeq}, issuers = ${issuers.toSeq}, engine = $engine")
    withKeyManagers { keyManager: X509KeyManager =>
      keyManager match {
        case extendedKeyManager: X509ExtendedKeyManager =>
          val clientAlias = extendedKeyManager.chooseEngineClientAlias(keyType, issuers, engine)
          if (clientAlias != null) {
            logger.debug(s"chooseEngineClientAlias: using clientAlias $clientAlias with keyManager $extendedKeyManager")
            return clientAlias
          }
        case _ =>
        // do nothing
      }
    }
    null
  }

  override def chooseEngineServerAlias(keyType: String, issuers: Array[Principal], engine: SSLEngine): String = {
    logger.debug(s"chooseEngineServerAlias: keyType = ${keyType.toSeq}, issuers = ${issuers.toSeq}, engine = $engine")

    withKeyManagers { keyManager: X509KeyManager =>
      keyManager match {
        case extendedKeyManager: X509ExtendedKeyManager =>
          val clientAlias = extendedKeyManager.chooseEngineServerAlias(keyType, issuers, engine)
          if (clientAlias != null) {
            logger.debug(s"chooseEngineServerAlias: using clientAlias $clientAlias with keyManager $extendedKeyManager")
            return clientAlias
          }
        case _ =>
        // do nothing
      }
    }
    null
  }

  def getServerAliases(keyType: String, issuers: Array[Principal]): Array[String] = {
    logger.debug(s"getServerAliases: keyType = $keyType, issuers = ${issuers.toSeq}")

    val serverAliases = new ArrayBuffer[String]
    withKeyManagers { keyManager =>
      val aliases = keyManager.getServerAliases(keyType, issuers)
      if (aliases != null) {
        serverAliases.appendAll(aliases)
      }
    }
    logger.debug(s"getServerAliases: serverAliases = $serverAliases")

    nullIfEmpty(serverAliases.toArray)
  }

  def chooseServerAlias(keyType: String, issuers: Array[Principal], socket: Socket): String = {
    logger.debug(s"chooseServerAlias: keyType = $keyType, issuers = ${issuers.toSeq}, socket = $socket")
    withKeyManagers { keyManager =>
      val serverAlias = keyManager.chooseServerAlias(keyType, issuers, socket)
      if (serverAlias != null) {
        logger.debug(s"chooseServerAlias: using serverAlias $serverAlias with keyManager $keyManager")
        return serverAlias
      }
    }
    null
  }

  def getCertificateChain(alias: String): Array[X509Certificate] = {
    logger.debug(s"getCertificateChain: alias = $alias")
    withKeyManagers { keyManager =>
      val chain = keyManager.getCertificateChain(alias)
      if (chain != null && chain.length > 0) {
        logger.debug(s"getCertificateChain: chain ${debugChain(chain)} with keyManager $keyManager")
        return chain
      }
    }
    null
  }

  def getPrivateKey(alias: String): PrivateKey = {
    logger.debug(s"getPrivateKey: alias = $alias")
    withKeyManagers { keyManager =>
      val privateKey = keyManager.getPrivateKey(alias)
      if (privateKey != null) {
        logger.debug(s"getPrivateKey: privateKey $privateKey with keyManager $keyManager")
        return privateKey
      }
    }
    null
  }

  private def withKeyManagers[T](block: (X509KeyManager => T)): Seq[CertificateException] = {
    val exceptionList = ArrayBuffer[CertificateException]()
    keyManagers.foreach { keyManager =>
      try {
        block(keyManager)
      } catch {
        case certEx: CertificateException =>
          exceptionList.append(certEx)
      }
    }
    exceptionList
  }

  private def nullIfEmpty[T](array: Array[T]) = if (array.size == 0) null else array

  override def toString = {
    s"CompositeX509KeyManager(keyManagers = [$keyManagers])"
  }
}
