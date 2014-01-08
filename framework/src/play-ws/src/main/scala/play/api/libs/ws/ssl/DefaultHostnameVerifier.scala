/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
  // We are using SSLEngine directly, so these rules apply.
  //
  // Ideally we want something compliant with RFC 2818 or RFC 6125, but nothing is compliant with RFC 6125 yet.
  //
  // There are a few options:
  //
  // 1) Use org.apache.http.conn.ssl.StrictHostnameVerifier
  // 3) Follow the HttpsURLConnection method:
  //      1.6: Enable trySetHostnameVerification directly (will call HostnameChecker under the hood)
  //      1.7: Enable sslParams.setEndpointIdentificationAlgorithm("HTTPS") in 1.7 (will also call HostnameChecker)
  // 2) Leverage HostnameChecker directly in a HostnameVerifier
  //
  // 1 means setting up a different library.  However, the code looks very complete.
  // 2, we cant use in 1.7 due to NPE, and the fix would involve disabling the connection pool and having client per URL.
  // 3, we can do.
  //
  // NOTE: hostname verification is not always desired, especially when using custom certificates:
  //
  // From http://docs.fedoraproject.org/en-US/Fedora_Security_Team//html/Defensive_Coding/sect-Defensive_Coding-TLS-Client-OpenJDK.html
  // "When certificate overrides are in place, host name verification should not be performed because there is no
  // security requirement that the host name in the certificate matches the host name used to establish the connection
  // (and it often will not). However, without host name verification, it is not possible to perform transparent
  // fallback to certification validation using the system certificate store."
  //
  // I think this is wussing out, because you can totally set the CN using -ext using keytool in 1.7, but whatever.
  //
  // Also see http://stackoverflow.com/questions/18139448/how-should-i-do-hostname-validation-when-using-jsse
  // http://stackoverflow.com/questions/8443081/how-are-ssl-certificate-server-names-resolved-can-i-add-alternative-names-using
  // http://stackoverflow.com/questions/17943286/get-list-of-all-hostnames-matching-certificate/17948270#17948270
  // https://github.com/ppelleti/host-vfy

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
              logger.debug("verify: Certificate does not match hostname", e)
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
