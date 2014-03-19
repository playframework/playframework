/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import java.security.cert.{CertPathValidatorException, X509Certificate, Certificate, PKIXCertPathChecker}
import javax.net.ssl.{X509TrustManager, TrustManagerFactory}
import java.security.KeyStore
import CertificateGenerator._


object CertificateValidatorSpec extends Specification {

  "CertificateValidator" should {

    def defaultTrustedCertificates : Array[X509Certificate] = {
      val factory = TrustManagerFactory.getInstance("PKIX")
      // Uses the default values for the trust manager...
      // http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#X509TrustManager
      factory.init(null.asInstanceOf[KeyStore])
      val trustManager = factory.getTrustManagers.head.asInstanceOf[X509TrustManager]
      val trustedCerts = trustManager.getAcceptedIssuers
      trustedCerts
    }

    "throw exception when the trusted certificate is not a CA certificate" in {
      val caCert = generateRSAWithSHA256()
      val clientCert = generateRSAWithSHA256()
      val chain = Array[X509Certificate](
        clientCert,
        caCert
      )

      val signatureConstraints = Set(AlgorithmConstraint("md5"))
      val keyConstraints = Set(AlgorithmConstraint(""))
      val revocationEnabled = false
      val revocationLists = None
      val validator = new CertificateValidator(signatureConstraints, keyConstraints, revocationEnabled, revocationLists)

      val trustedCerts = Array(caCert)
      validator.validate(chain, trustedCerts).must(throwA[CertPathValidatorException]("basic constraints check failed: this is not a CA certificate"))
    }

    "throw an exception with an unrecognized chain in the root" in {
      val pretendingToBeACACert = generateRSAWithSHA256()
      val clientCert = generateRSAWithSHA256()

      val chain = Array[X509Certificate](
        clientCert,
        pretendingToBeACACert
      )

      val signatureConstraints = Set(AlgorithmConstraint("md5"))
      val keyConstraints = Set(AlgorithmConstraint(""))
      val revocationEnabled = false
      val revocationLists = None
      val validator = new CertificateValidator(signatureConstraints, keyConstraints, revocationEnabled, revocationLists)

      // pretendingToBeACACert is not one of the trusted certs
      val trustedCerts = defaultTrustedCertificates
      validator.validate(chain, trustedCerts).must(throwA[CertPathValidatorException]("Path does not chain with any of the trust anchors"))
    }

    "Throw an exception with a random CA cert as the root" in {
      val clientCert = generateRSAWithSHA256()
      val trustedCerts = defaultTrustedCertificates

      val chain = Array[X509Certificate](
        clientCert,
        trustedCerts(0)
      )

      val signatureConstraints = Set(AlgorithmConstraint("md5"))
      val keyConstraints = Set(AlgorithmConstraint(""))
      val revocationEnabled = false
      val revocationLists = None
      val validator = new CertificateValidator(signatureConstraints, keyConstraints, revocationEnabled, revocationLists)

      validator.validate(chain, trustedCerts).must(throwA[CertPathValidatorException]("subject/issuer name chaining check failed"))
    }

    "should just work with a self signed certificate" in {
      val clientCert = generateRSAWithSHA256()
      val trustedCerts = Array(clientCert)

      val chain = Array(clientCert)

      val signatureConstraints = Set(AlgorithmConstraint("md5"))
      val keyConstraints = Set(AlgorithmConstraint(""))
      val revocationEnabled = false
      val revocationLists = None
      val validator = new CertificateValidator(signatureConstraints, keyConstraints, revocationEnabled, revocationLists)

      val result = validator.validate(chain, trustedCerts)
      result must not beNull
    }

    "work when there is a revocation list" in todo

    "throw exception when the site is found in the revocation list" in todo

    "throw exception when there is a certificate chain with an unrecognized critical certificate extension" in todo

    "throw exception when one of the certificates in the chain has expired" in {
      todo
    }

    "throw exception when a custom checker fails" in {
      todo
    }

  }
}
