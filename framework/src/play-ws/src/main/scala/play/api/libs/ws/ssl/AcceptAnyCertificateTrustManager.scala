/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ssl

import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Accepts any certificate.
 */
class AcceptAnyCertificateTrustManager extends X509TrustManager {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def getAcceptedIssuers: Array[X509Certificate] = {
    new Array[X509Certificate](0)
  }

  def checkClientTrusted(certs: Array[X509Certificate], authType: String) {
    logger.warn(s"checkClientTrusted: accepting certs ${certs.toSeq}")
  }

  def checkServerTrusted(certs: Array[X509Certificate], authType: String) {
    logger.warn(s"checkServerTrusted: accepting certs ${certs.toSeq}")
  }

}
