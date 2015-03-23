package play.api.libs.ws.ssl

import javax.net.ssl.{ SSLSession, HostnameVerifier }

import org.slf4j.LoggerFactory

/**
 * Add a disabled but complaining hostname verifier.
 */
class DisabledComplainingHostnameVerifier extends HostnameVerifier {

  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ssl.DisabledComplainingHostnameVerifier")

  private val defaultHostnameVerifier = new DefaultHostnameVerifier()

  override def verify(hostname: String, sslSession: SSLSession): Boolean = {
    val hostNameMatches = defaultHostnameVerifier.verify(hostname, sslSession)
    if (!hostNameMatches) {
      val msg = s"""Hostname verification failed on hostname $hostname, but the connection was accepted because ws.ssl.disableHostnameVerification is enabled.  Please fix the X.509 certificate on the host to remove this warning."""
      logger.warn(msg)
    }
    true
  }
}
