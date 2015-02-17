/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.net.ssl.X509TrustManager
import java.security.cert._
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal
import java.security.GeneralSecurityException

/**
 * A trust manager that is a composite of several smaller trust managers.   It is responsible for verifying the
 * credentials received from a peer.
 */
class CompositeX509TrustManager(trustManagers: Seq[X509TrustManager], algorithmChecker: AlgorithmChecker) extends X509TrustManager {

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def getAcceptedIssuers: Array[X509Certificate] = {
    logger.debug("getAcceptedIssuers: ")
    val certificates = ArrayBuffer[X509Certificate]()
    val exceptionList = withTrustManagers {
      trustManager =>
        certificates.appendAll(trustManager.getAcceptedIssuers)
    }
    // getAcceptedIssuers should never throw an exception.
    if (!exceptionList.isEmpty) {
      val msg = exceptionList(0).getMessage
      throw new CompositeCertificateException(msg, exceptionList.toArray)
    }
    certificates.toArray
  }

  // In 1.6, sun.security.ssl.X509TrustManagerImpl extends from com.sun.net.ssl.internal.ssl.X509ExtendedTrustManager
  // In 1.7, sun.security.ssl.X509TrustManagerImpl extends from javax.net.ssl.X509ExtendedTrustManager.
  // The two X509ExtendedTrustManager contain different method signatures, and both are available in 1.7, which means
  // it's really hard to keep something backwards compatible if something is calling trustManager.asInstanceOf[X509ExtendedTrustManager]
  // internally.  For now, we have to trust that the internal API holds to the X509TrustManager interface.
  //
  //def checkClientTrusted(chain: Array[X509Certificate], authType: String, hostname: String, algorithm: String): Unit = ???
  //def checkServerTrusted(chain: Array[X509Certificate], authType: String, hostname: String, algorithm: String): Unit = ???

  def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
    logger.debug("checkClientTrusted: chain = {}", debugChain(chain))

    val anchor: TrustAnchor = new TrustAnchor(chain(chain.length - 1), null)
    logger.debug(s"checkClientTrusted: checking key size only on root anchor $anchor")
    algorithmChecker.checkKeyAlgorithms(anchor.getTrustedCert)

    var trusted = false
    val exceptionList = withTrustManagers {
      trustManager =>
        trustManager.checkClientTrusted(chain, authType)
        logger.debug(s"checkClientTrusted: trustManager $trustManager found a match for ${debugChain(chain)}")
        trusted = true
    }

    if (!trusted) {
      val msg = "No trust manager was able to validate this certificate chain."
      throw new CompositeCertificateException(msg, exceptionList.toArray)
    }
  }

  def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {
    logger.debug(s"checkServerTrusted: chain = ${debugChain(chain)}, authType = $authType")

    // Trust anchor is at the end of the chain... there is no way to pass a trust anchor
    // through to a checker in PKIXCertPathValidator.doValidate(), so the trust manager is the
    // last place we have access to it.
    val anchor: TrustAnchor = new TrustAnchor(chain(chain.length - 1), null)
    logger.debug(s"checkServerTrusted: checking key size only on root anchor $anchor")
    algorithmChecker.checkKeyAlgorithms(anchor.getTrustedCert)

    var trusted = false
    val exceptionList = withTrustManagers {
      trustManager =>
        // always run through the trust manager before making any decisions
        trustManager.checkServerTrusted(chain, authType)
        logger.debug(s"checkServerTrusted: trustManager $trustManager using authType $authType found a match for ${debugChain(chain).toSeq}")
        trusted = true
    }

    if (!trusted) {
      val msg = s"No trust manager was able to validate this certificate chain: # of exceptions = ${exceptionList.size}"
      throw new CompositeCertificateException(msg, exceptionList.toArray)
    }
  }

  private def withTrustManagers(block: (X509TrustManager => Unit)): Seq[Throwable] = {
    val exceptionList = ArrayBuffer[Throwable]()
    trustManagers.foreach {
      trustManager =>
        try {
          block(trustManager)
        } catch {
          case e: CertPathBuilderException =>
            logger.debug("No path found to certificate: this usually means the CA is not in the trust store", e)
            exceptionList.append(e)
          case e: GeneralSecurityException =>
            logger.debug("General security exception", e)
            exceptionList.append(e)
          case NonFatal(e) =>
            logger.debug("Unexpected exception!", e)
            exceptionList.append(e)
        }
    }
    exceptionList
  }

  override def toString = {
    s"CompositeX509TrustManager(trustManagers = [$trustManagers], algorithmChecker = $algorithmChecker)"
  }
}
