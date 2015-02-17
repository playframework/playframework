/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.cert.{ CertificateException, X509Certificate }
import javax.net.ssl.{ HostnameVerifier, SSLPeerUnverifiedException, SSLSession }
import javax.security.auth.kerberos.KerberosPrincipal
import sun.security.util.HostnameChecker
import org.slf4j.LoggerFactory
import com.ning.http.util.Base64
import java.security.Principal

/**
 * Use the internal sun hostname checker as the hostname verifier.  Thanks to Kevin Locke.
 *
 * @see sun.security.util.HostnameChecker
 * @see http://kevinlocke.name/bits/2012/10/03/ssl-certificate-verification-in-dispatch-and-asynchttpclient/
 */
class DefaultHostnameVerifier extends HostnameVerifier {

  // AsyncHttpClient issue #197: "SSL host name verification disabled by default"
  // https://github.com/AsyncHttpClient/async-http-client/issues/197
  //
  // From http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#HostnameVerifier
  //
  // "When using raw SSLSockets/SSLEngines you should always check the peer's credentials before sending any data.
  // The SSLSocket and SSLEngine classes do not automatically verify that the hostname in a URL matches the
  // hostname in the peer's credentials. An application could be exploited with URL spoofing if the hostname is
  // not verified."
  //
  // We are using SSLEngine directly, so we have to use the AsyncHttpClient Netty Provider to provide hostnname
  // verification.

  private val logger = LoggerFactory.getLogger(getClass)

  def hostnameChecker: HostnameChecker = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS)

  def matchKerberos(hostname: String, principal: Principal) = HostnameChecker.`match`(hostname, principal.asInstanceOf[KerberosPrincipal])

  def isKerberos(principal: Principal): Boolean = principal != null && principal.isInstanceOf[KerberosPrincipal]

  def verify(hostname: String, session: SSLSession): Boolean = {
    logger.debug(s"verify: hostname = $hostname, sessionId (base64) = ${Base64.encode(session.getId)}")

    val checker = hostnameChecker
    val result = try {
      session.getPeerCertificates match {
        case Array(cert: X509Certificate, _*) =>
          try {
            checker.`match`(hostname, cert)
            // Certificate matches hostname
            true
          } catch {
            case e: CertificateException =>
              // Certificate does not match hostname
              val subjectAltNames = cert.getSubjectAlternativeNames
              logger.debug(s"verify: Certificate does not match hostname! subjectAltNames = $subjectAltNames, hostName = $hostname", e)
              false
          }

        case notMatch =>
          // Peer does not have any certificates or they aren't X.509
          logger.debug(s"verify: Peer does not have any certificates: $notMatch")
          false
      }
    } catch {
      case _: SSLPeerUnverifiedException =>
        // Not using certificates for verification, try verifying the principal
        try {
          val principal = session.getPeerPrincipal
          if (isKerberos(principal)) {
            matchKerberos(hostname, principal)
          } else {
            // Can't verify principal, not Kerberos
            logger.debug(s"verify: Can't verify principal, not Kerberos")
            false
          }
        } catch {
          case e: SSLPeerUnverifiedException =>
            // Can't verify principal, no principal
            logger.debug("Can't verify principal, no principal", e)
            false
        }
    }
    logger.debug("verify: returning {}", result)
    result
  }

}
