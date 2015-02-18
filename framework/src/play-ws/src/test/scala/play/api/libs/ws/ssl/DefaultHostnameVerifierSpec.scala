/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._

import org.specs2.mock._

import javax.net.ssl.{ SSLPeerUnverifiedException, SSLSession }
import sun.security.util.HostnameChecker
import java.security.cert.{ CertificateException, X509Certificate }
import java.security.Principal
import javax.security.auth.kerberos.KerberosPrincipal

object DefaultHostnameVerifierSpec extends Specification with Mockito {

  "DefaultHostnameVerifier" should {

    "work" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
      }

      session.getId returns "1234".getBytes("UTF-8")
      val cert = mock[X509Certificate]
      session.getPeerCertificates returns Array(cert)

      val actual = verifier.verify("example.com", session)

      actual must beTrue
      got {
        mockChecker.`match`("example.com", cert)
      }
    }

    "fail when the match fails" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]
      val cert = mock[X509Certificate]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
      }

      session.getId returns "1234".getBytes("UTF-8")
      session.getPeerCertificates returns Array(cert)
      mockChecker.`match`("example.com", cert) throws (new CertificateException("derp"))

      val actual = verifier.verify("example.com", session)

      actual must beFalse
    }

    "fail when there are no peer certificates" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]
      val cert = mock[X509Certificate]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
      }

      session.getId returns "1234".getBytes("UTF-8")
      session.getPeerCertificates returns null // very rude, but that's Java APIs for you
      mockChecker.`match`("example.com", cert) throws (new CertificateException("derp"))

      val actual = verifier.verify("example.com", session)

      actual must beFalse
    }

    "succeeds when the peer is unverified but the peer principal is there and kerberous" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]
      val principal = mock[Principal]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
        override def isKerberos(principal: Principal) = true
        override def matchKerberos(hostname: String, principal: Principal) = true
      }

      session.getId returns "1234".getBytes("UTF-8")
      session.getPeerCertificates throws new SSLPeerUnverifiedException("derp")
      session.getPeerPrincipal returns principal

      val actual = verifier.verify("example.com", session)

      actual must beTrue
    }

    "fails when the peer is unverified but the peer principal is there (but is not a match)" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]
      val principal = mock[Principal]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
        override def isKerberos(principal: Principal) = true
        override def matchKerberos(hostname: String, principal: Principal) = false
      }

      session.getId returns "1234".getBytes("UTF-8")
      session.getPeerCertificates throws new SSLPeerUnverifiedException("derp")
      session.getPeerPrincipal returns principal

      val actual = verifier.verify("example.com", session)

      actual must beFalse
    }

    "fails when the peer is unverified but the peer principal is there (but is not a KerberosPrincipal)" in {
      val session = mock[SSLSession]
      val mockChecker = mock[HostnameChecker]
      val principal = mock[Principal]

      val verifier = new DefaultHostnameVerifier() {
        override def hostnameChecker = mockChecker
        override def isKerberos(principal: Principal) = false
      }

      session.getId returns "1234".getBytes("UTF-8")
      session.getPeerCertificates throws new SSLPeerUnverifiedException("derp")
      session.getPeerPrincipal returns principal

      val actual = verifier.verify("example.com", session)

      actual must beFalse
    }

  }

}
