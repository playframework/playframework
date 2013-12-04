/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
class CompositeX509TrustManager(trustManagers: Seq[X509TrustManager], certificateValidator:CertificateValidator) extends X509TrustManager {

  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
    logger.debug("checkClientTrusted: chain = {}", debugChain(chain))

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

  def getAcceptedIssuers: Array[X509Certificate] = {
    logger.debug("getAcceptedIssuers: ")
    val certificates = ArrayBuffer[X509Certificate]()
    val exceptionList = withTrustManagers {
      trustManager =>
        certificates.appendAll(trustManager.getAcceptedIssuers)
    }
    if (!exceptionList.isEmpty) {
      logger.warn("getAcceptedIssuers, exceptionList = {}", exceptionList)
    }
    certificates.toArray
  }

  def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {
    logger.debug(s"checkServerTrusted: chain = ${debugChain(chain)}, authType = $authType")

    var trusted = false
    val exceptionList = withTrustManagers {
      trustManager =>
        // always run through the trust manager before making any decisions
        trustManager.checkServerTrusted(chain, authType)
        logger.debug(s"checkServerTrusted: trustManager $trustManager using authType $authType found a match for ${debugChain(chain).toSeq}")

        // Run through the custom path checkers...
        certificateValidator.validate(chain, trustManager.getAcceptedIssuers)

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
          case e:CertPathBuilderException =>
            logger.debug("No path found to certificate: this usually means the CA is not in the trust store", e)
            exceptionList.append(e)
          case e:GeneralSecurityException =>
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
    s"CompositeX509TrustManager(trustManagers = [$trustManagers])"
  }

}
