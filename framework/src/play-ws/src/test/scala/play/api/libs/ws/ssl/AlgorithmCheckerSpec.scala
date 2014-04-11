/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import java.security.cert.{CertPathValidatorException, Certificate}
import java.util.Collections._
import AlgorithmConstraintsParser._

object AlgorithmCheckerSpec extends Specification {

  "AlgorithmChecker" should {

    "pass a good key algorithm (RSA > 1024)" in {
      val disabledSignatureAlgorithms = parseAll(line, "").get.toSet
      val disabledKeyAlgorithms = parseAll(line, "RSA keySize < 1024").get.toSet
      val checker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)
      val certificate:Certificate = CertificateGenerator.generateRSAWithSHA256(2048)

      checker.check(certificate, emptySet())
      success
    }

    "fail a weak key algorithm (RSA < 512)" in {
      val disabledSignatureAlgorithms = parseAll(line, "").get.toSet
      val disabledKeyAlgorithms = parseAll(line, "RSA keySize < 1024").get.toSet
      val checker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val certificate:Certificate = CertificateGenerator.generateRSAWithSHA256(512)

      checker.check(certificate, emptySet()).must(throwA[CertPathValidatorException])
    }

    "pass a good signature algorithm (SHA256)" in {
      val disabledSignatureAlgorithms = parseAll(line, "MD5").get.toSet
      val disabledKeyAlgorithms = parseAll(line, "").get.toSet
      val checker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val certificate:Certificate = CertificateGenerator.generateRSAWithSHA256(512)

      checker.check(certificate, emptySet())
      success
    }

    "fail a bad signature algorithm (MD5)" in {
      val disabledSignatureAlgorithms = parseAll(line, "MD5").get.toSet
      val disabledKeyAlgorithms = parseAll(line, "").get.toSet
      val checker = new AlgorithmChecker(disabledSignatureAlgorithms, disabledKeyAlgorithms)

      val intermediateCert:Certificate = CertificateGenerator.generateRSAWithMD5(2048)
      //val eeCert:Certificate = CertificateGenerator.generateRSAWithSHA256(2048)

      checker.check(intermediateCert, emptySet()).must(throwA[CertPathValidatorException])
    }
  }
}
